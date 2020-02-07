package com.kars.sihpoc;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class Classify extends AppCompatActivity {

    // presets for rgb conversion
    private static final int RESULTS_TO_SHOW = 3;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    // options for model interpreter
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    // tflite graph
    private Interpreter tflite;
    // holds all the possible labels for model
    private List<String> labelList;
    // holds the selected image data as bytes
    private ByteBuffer imgData = null;
    // holds the probabilities of each label for non-quantized graphs
    private float[][] labelProbArray = null;
    // array that holds the labels with the highest probabilities
    private String[] topLables = null;
    // array that holds the highest probabilities
    private String[] topConfidence = null;


    // selected classifier information received from extras
    private String chosen;
    private String labelsFile;
    private boolean isCropDetection;

    // input image dimensions for the Inception Model
    private int DIM_IMG_SIZE_X = 4;
    private int DIM_IMG_SIZE_Y = 4;
    private int DIM_PIXEL_SIZE = 512;

    // int array to hold image data
    private int[] intValues;

    // activity elements
    private ImageView selected_image;
    private Button classify_button;
    private TextView txtCropName, txtGrowthTime, txtMarketRate, txtRotationCrops, txtYield, txtSoilType, txtDiseaseName, txtDiseaseInfo;
    private String file_path = null;

    private LinearLayout llCropDetection, llDiseaseDetection;
    private HashMap<String, String> diseaseMap;

    // priority queue that will hold the top results from the CNN
    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // get all selected classifier data from classifiers
        chosen = (String) getIntent().getStringExtra("chosen");
        labelsFile = (String) getIntent().getStringExtra("labels");
        isCropDetection = (boolean) getIntent().getBooleanExtra("cropDetection", true);

        // initialize array that holds image data
        intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

        super.onCreate(savedInstanceState);

        //initilize graph and labels
        try {
            tflite = new Interpreter(loadModelFile(), tfliteOptions);
            labelList = loadLabelList();
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e("Error Model", ex.getMessage());
        }

        // initialize byte array. The size depends if the input data needs to be quantized or not. Here it is float
        imgData =
                ByteBuffer.allocateDirect(
                        4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);

        imgData.order(ByteOrder.nativeOrder());

        // initialize probabilities array. The datatypes that array holds depends if the input data needs to be quantized or not
        //Here it is float
        labelProbArray = new float[1][labelList.size()];

        setContentView(R.layout.activity_classify);

        txtCropName = findViewById(R.id.txtCropName);
        txtGrowthTime = findViewById(R.id.txtGrowthTime);
        txtMarketRate = findViewById(R.id.txtMarketRate);
        txtRotationCrops = findViewById(R.id.txtRotationCrops);
        txtYield = findViewById(R.id.txtYield);
        txtSoilType = findViewById(R.id.txtSoilType);
        llCropDetection = findViewById(R.id.llCropDetection);
        llDiseaseDetection = findViewById(R.id.llDiseaseDetection);
        txtDiseaseName = findViewById(R.id.txtDiseaseName);
        txtDiseaseInfo = findViewById(R.id.txtDiseaseInfo);

        // initialize imageView that displays selected image to the user
        selected_image = (ImageView) findViewById(R.id.selected_image);

        // initialize array to hold top labels
        topLables = new String[RESULTS_TO_SHOW];
        // initialize array to hold top probabilities
        topConfidence = new String[RESULTS_TO_SHOW];
        // classify current dispalyed image
        classify_button = (Button) findViewById(R.id.classify_image);

        if(isCropDetection) {
            llCropDetection.setVisibility(View.VISIBLE);
            classify_button.setText("Detect!");
        }else {
            llDiseaseDetection.setVisibility(View.VISIBLE);
            classify_button.setText("Diagnose!");
            diseaseMap = new HashMap<>();
            diseaseMap.put("Brown Spot", "Monitor soil nutrients regularly. Apply required fertilizers. For soils that are low in silicon, apply calcium silicate slag before planting. Use resistant varieties.");
            diseaseMap.put("Cercospara Leaf Spot", "Genetic Resistance. Susceptible cultivars should not be planted within 100 yards of the previous year's infected crop. Many fungicides are available for managing the disease. However, fungicide resistance management must also be considered and monitored carefully.");
            diseaseMap.put("Common Rust", "Apply copper sprays or sulphur powders to prevent infection. Avoid wetting the leaves when watering plants. Space your plants properly to encourage good air circulation.");
            diseaseMap.put("Northern Leaf Blight", "Hybrid selection. Scouting. Scout for symptoms of corn leaf blight when ideal environmental conditions favour disease development, especially during or before pollination. Cultural practices. Crop rotation remains a solid tactic to help diminish disease threats.");
            diseaseMap.put("Hispa", "Avoid over fertilizing the field. Close plant spacing results in greater leaf densities that can tolerate higher hispa numbers. Leaf tip containing blotch mines should be destroyed.");
            diseaseMap.put("Leaf Blast", "Plant the least-susceptible varieties and use a broad-spectrum seed treatment. Grow rice in fields where flood levels are easily maintained. Damage from blast can be reduced by keeping soil flooded 2 to 4 inches deep from the time rice plants are 6 to 8 inches tall until draining for harvest.");
            diseaseMap.put("Yellow Leaf Curl Virus", "The most effective treatments used to control the spread of Yellow Leaf Curl Virus are insecticides and resistant crop varieties, Planting resistant/tolerant lines, crop rotation, and breeding for resistance, production of transgenic tomato plants resistant to Tomato yellow leaf curl virus." );
            diseaseMap.put("Spider Mites", "lant resistant varieties are available, Monitor field regularly and check underside of leaves, Remove affected leaves, Water your crops regularly to as stressed trees and plants are less tolerant to spider mite damage, Control the use of Insecticides to allow beneficial insects to thrive. ");
            diseaseMap.put("Sepotoria Leaf Spot","Remove infected leaves immediately, and be sure to wash your hands thoroughly before working with uninfected plants, Consider organic fungi sides containing either copper or potassium bicarbonate, Use Fungonil or Daconi.");
            diseaseMap.put("Early Bight","Prune or stake plants to improve air circulation and reduce fungal problems, Keep the soil under plants clean and free of garden debris. Add a layer of organic compost to prevent the spores from splashing back up onto vegetation, Drip irrigation and soaker hoses can be used to help keep the foliage dry, apply copper-based fungicides early, two weeks before disease normally appears or when weather forecasts predict a long period of wet weather, Burn or bag infected plant parts. Do NOT compost.");
            diseaseMap.put("Late Blight","Plant resistant cultivars when available, Remove volunteers from the garden prior to planting and space plants far enough apart to allow for plenty of air circulation, Water in the early morning hours, or use soaker hoses, to give plants time to dry out during the day — avoid overhead irrigation, Destroy all tomato debris after harvest. ");
            diseaseMap.put("Target Spot","Pay careful attention to air circulation, as target spot of tomato thrives in humid conditions, Grow the plants in full sunlight. Be sure the plants aren’t crowded and that each tomato has plenty of air circulation. Cage or stake tomato plants to keep the plants above the soil, Water tomato plants in the morning so the leaves have time to dry. Water at the base of the plant or use a soaker hose or drip system to keep the leaves dry");
            diseaseMap.put("Leaf Mold","Management practices for leaf mold include managing humidity, changing the location where tomatoes are grown, selecting resistant or less susceptible varieties, applying fungicides, and removing tomato plant debris after last harvest or incorporating it deeply into the soil, mprove air movement around plants by planting with good spacing between plants, locating rows parallel to the prevalent wind direction in an open area, staking or trellising plants, and pruning excess branches and dead leaves, Provide water to the base of plants rather than using sprinklers that wet leaves.");
            diseaseMap.put("Bacterial Spot","Use Copper-containing bactericides applied on dry seedlings before transplanting to production fields may be effective. e. The material will kill only those bacteria on the surface of the leaf and not within the leaf tissue. the sprays should be started a few days after emergence, continued at 5- day intervals, applied with equipment that ensures good coverage, and applied on dry plants, Avoid overhead irrigation or rain for 24 hr after application.");
            diseaseMap.put("Mosaic Virus","The only treatment is prevention. No chemical products are available to cure or protect plants, The best factor in controlling and reducing infection is to practice sanitation, Remove any infected plants, including the roots, Remove Also, discard any plants near those affected,  Gardening tools, pots, and planters need to be sterilized and washed regularly, Steam or commercial disinfectants may also be used for disinfection");
        }

        classify_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get current bitmap from imageView
                Bitmap bitmap_orig = ((BitmapDrawable) selected_image.getDrawable()).getBitmap();
                // resize the bitmap to the required input size to the CNN
                Bitmap bitmap = getResizedBitmap(bitmap_orig, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
                // convert bitmap to byte array
                convertBitmapToByteBuffer(bitmap);
                // pass byte data to the graph
                tflite.run(imgData, labelProbArray);
                // display the results
                printTopKLabels();
            }
        });

        // get image from previous activity to show in the imageView
        Uri uri = (Uri) getIntent().getParcelableExtra("resID_uri");
        file_path = getIntent().getStringExtra("file-path");
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            selected_image.setImageBitmap(bitmap);
            // not sure why this happens, but without this the image appears on its side
            selected_image.setRotation(selected_image.getRotation());
            saveImageInFile(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveImageInFile(Bitmap bm) {
        File file = new File(file_path);
        if (!file.exists()) {
            Log.e("ERROR", " in file_provicer");
        } else {
            try {
                OutputStream os = new FileOutputStream(file);
                bm.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.flush();
                os.close();
            } catch (IOException e) {
                Log.e("EXCEPTION", e.toString());
            }
        }
    }

    private void printArray(){
        String ans = "";
        for(int i = 0;i<labelProbArray.length;i++){
            for(int j = 0;j<labelProbArray[0].length;j++){
                ans += labelProbArray[i][j] + " ";
            }
            ans += "\n";
        }
        Log.e("Prob Array", ans);
    }

    // loads tflite grapg from file
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(chosen);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // converts bitmap to byte array which is passed in the tflite graph
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // loop through all pixels
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                // get rgb values from intValues where each int holds the rgb values for a pixel.
                // if quantized, convert each rgb value to a byte, otherwise to a float
                imgData.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((val) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }
    }

    // loads the labels from the label txt file in assets into a string array
    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(this.getAssets().open(labelsFile)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    // print the top labels and respective confidences
    private void printTopKLabels() {
        printArray();
        // add all results to priority queue
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i), labelProbArray[0][i]));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }

        // get top results from priority queue
        final int size = sortedLabels.size();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            topLables[i] = label.getKey();
            topConfidence[i] = String.format("%.0f%%", label.getValue() * 100);
        }

        if(isCropDetection) {
            txtCropName.setText(topLables[2]);
            getAndFillCropDetails(topLables[2]);
        }else{
            txtDiseaseName.setText(topLables[2]);
            Log.e("LABELS", topLables[2] + " : " + diseaseMap.containsKey(topLables[2]) + " : " + topLables[2]);
            if(diseaseMap!=null && diseaseMap.containsKey(topLables[2])){
                txtDiseaseInfo.setText(diseaseMap.get(topLables[2]));
            }
        }
    }

    private void getAndFillCropDetails(String cropName){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        DatabaseReference loc = ref.child("Crops").child(cropName);
        loc.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                CropDetails cropDetails = dataSnapshot.getValue(CropDetails.class);
                Log.d("Crop-Details", cropDetails.toString());
                assert cropDetails!=null;
                txtGrowthTime.setText(cropDetails.getGrowthTime());
                txtMarketRate.setText(cropDetails.getMarketRate());
                txtRotationCrops.setText(cropDetails.getRotationCrops());
                txtYield.setText(cropDetails.getYield());
                txtSoilType.setText(cropDetails.getSoilType());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // resizes bitmap to given dimensions
    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }
}
