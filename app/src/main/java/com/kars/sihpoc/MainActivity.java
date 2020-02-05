package com.kars.sihpoc;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnSuccessListener;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//Todo Location still not stable
public class MainActivity extends AppCompatActivity implements LocationListener {
    private CardView cardCropIdentify, cardDiseaseIdentify, cardWeather;
    private TextView txtNoGPS;
    private ProgressBar progressWeather;
    private LinearLayout llWeather;
    private static final int REQUEST_CAMERA = 101, REQUEST_STORAGE_PERMISSION = 102,
            REQUEST_LOCATION_PERMISSION = 103, REQUEST_CHECK_SETTINGS = 104;
    public static FusedLocationProviderClient locationProviderClient;
    protected LocationManager locationManager;
    private String filePath = null;
    private String BaseUrl = "https://api.openweathermap.org/";
    private boolean showWeather = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cardCropIdentify = (CardView) findViewById(R.id.cardIdentifyCrop);
        cardDiseaseIdentify = (CardView) findViewById(R.id.cardCropDisease);
        cardWeather = (CardView) findViewById(R.id.cardWeather);
        progressWeather = (ProgressBar) findViewById(R.id.progress_weather);
        txtNoGPS = (TextView) findViewById(R.id.txtNoGps);
        llWeather = (LinearLayout) findViewById(R.id.llWeather);
        txtNoGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestLocationPermission();
            }
        });
        requestLocationPermission();
        cardCropIdentify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestStoragePermission();
            }
        });
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            openCameraIntent();
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            displayLocationSettingsRequest(this);
        }
    }

    // checks that the user has allowed all the required permission of read and write and camera. If not, notify the user and close the application
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                txtNoGPS.setVisibility(View.VISIBLE);
                progressWeather.setVisibility(View.GONE);
                llWeather.setVisibility(View.GONE);
            } else {
                displayLocationSettingsRequest(this);
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(getApplicationContext(), "This application needs read, write, and camera permissions to run. Application now closing.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                openCameraIntent();
            }
        }
    }

    // opens camera for user
    private void openCameraIntent() {
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e("EXCEPTION", ex.toString());
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.kars.sihpoc.fileprovider",
                    photoFile);
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .setCropShape(CropImageView.CropShape.RECTANGLE)
                    .start(this);
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // if the camera activity is finished, obtained the uri, crop it to make it square, and send it to 'Classify' activity
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK && result != null) {
                Uri uriCroppedImage = result.getUri();
                Intent intent = new Intent(this, Classify.class);
                intent.putExtra("resID_uri", uriCroppedImage);
                intent.putExtra("chosen", "model.tflite");
                intent.putExtra("file-path", filePath);
                startActivity(intent);
                finish();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.e("CROP", "onActivityResult: ", error);
            }
        } else if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK)
                showLocation();
            else
                txtNoGPS.setVisibility(View.VISIBLE);
        }
    }

    private void displayLocationSettingsRequest(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                String TAG = "Location-Settings";
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.e(TAG, "All location settings are satisfied.");
                        progressWeather.setVisibility(View.VISIBLE);
                        txtNoGPS.setVisibility(View.GONE);
                        showLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.e(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.e(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    private void showLocation() {
        locationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double lat = location.getLatitude();
                    double longitude = location.getLongitude();
                    getCurrentData(lat, longitude);
                    Log.e("Location", "Latitude : " + lat + "\nLongitude : " + longitude);
                } else {
                    progressWeather.setVisibility(View.VISIBLE);
                    txtNoGPS.setVisibility(View.GONE);
                    llWeather.setVisibility(View.GONE);
                    Log.e("LOCATION", "location is null");
                }
            }
        });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        filePath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onLocationChanged(Location location) {
        if(!showWeather){
            getCurrentData(location.getLatitude(), location.getLongitude());
            showWeather = true;
            Log.e("Change Location", "onLocationChanged: " + location);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude", "disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude", "enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude", "status");
    }

    void getCurrentData(Double lat, Double lon) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        WeatherService service = retrofit.create(WeatherService.class);
        Call<WeatherResponse> call = service.getCurrentWeatherData(lat.toString(), lon.toString(), getResources().getString(R.string.owa_id), "metric");
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.code() == 200) {
                    WeatherResponse weatherResponse = response.body();
                    assert weatherResponse != null;
                    fillWeatherCard(weatherResponse);
                }
                Log.d("API-RESPONSE", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Log.e("Error Api", t.getMessage());
                progressWeather.setVisibility(View.GONE);
                txtNoGPS.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Error fetching weather", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void fillWeatherCard(WeatherResponse wr){
        ImageView imgIcon = findViewById(R.id.imgWeatherIcon);
        TextView txtTemp = findViewById(R.id.txtTemp);
        TextView txtPressure = findViewById(R.id.txtPressure);
        TextView txtHumidity = findViewById(R.id.txtHumidity);
        TextView txtLat = findViewById(R.id.txtLatitude);
        TextView txtLong = findViewById(R.id.txtLongitude);
        txtTemp.setText(wr.main.temp + "\u2103");
        txtLat.setText(wr.coord.lat+"");
        txtLong.setText(wr.coord.lon+"");
        txtPressure.setText(wr.main.pressure+" hPa");
        txtHumidity.setText(wr.main.humidity+"%");
        Picasso.get().load("http://openweathermap.org/img/w/"+wr.weather.get(0).icon+".png").into(imgIcon, new com.squareup.picasso.Callback() {
            @Override
            public void onSuccess() {
                Log.e("Icon", "SUCCESS");
            }

            @Override
            public void onError(Exception e) {
                Log.e("Icon", "FAILURE : " + e.getMessage());
            }
        });
        llWeather.setVisibility(View.VISIBLE);
        progressWeather.setVisibility(View.GONE);
        txtNoGPS.setVisibility(View.GONE);
    }
}
