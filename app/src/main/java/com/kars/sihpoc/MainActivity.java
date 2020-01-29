package com.kars.sihpoc;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.soundcloud.android.crop.Crop;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private Button btnIdentify;
    private TextView txtWeatherAndLocation;
    private static final int REQUEST_CAMERA = 101;
    private String imgPath = null;
    private Uri imageURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnIdentify = findViewById(R.id.btnIdentify);
        txtWeatherAndLocation = findViewById(R.id.txtLocation);
        btnIdentify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCameraIntent();
            }
        });
    }

    // opens camera for user
    private void openCameraIntent(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        // tell camera where to store the resulting picture
        imageURI= getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // start camera, and wait for it to finish
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // if the camera activity is finished, obtained the uri, crop it to make it square, and send it to 'Classify' activity
        if(requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            try {
                Uri source_uri = imageURI;
                Uri dest_uri = Uri.fromFile(new File(getCacheDir(), "cropped"));
                // need to crop it to square image as CNN's always required square input
                Crop.of(source_uri, dest_uri).asSquare().start(MainActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // if cropping acitivty is finished, get the resulting cropped image uri and send it to 'Classify' activity
        else if(requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK){
            imageURI = Crop.getOutput(data);
            Intent i = new Intent(MainActivity.this, Classify.class);
            // put image data in extras to send
            i.putExtra("resID_uri", imageURI);
            // put filename in extras
            i.putExtra("chosen", "model.tflite");
            startActivity(i);
        }
    }
}
