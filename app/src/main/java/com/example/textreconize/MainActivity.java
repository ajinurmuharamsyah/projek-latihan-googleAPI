package com.example.textreconize;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.print.PageRange;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class MainActivity extends AppCompatActivity {

    //init Ui view
    private MaterialButton inputImageBtn, recognizedTextBtn;
    private ShapeableImageView imageIv;
    private EditText recognizedTextEt;

    //TAG
    private static final String TAG = "MAIN_TAG";

    //Uri of the image that we will take from camera/storage
    private Uri imageUri = null;

    //to handle the result of camera/storage permission
    private static final int CAMERA_REQUEST_CODE= 100;
    private static final int STORAGE_REQUEST_CODE= 101;

    //arrays of permission required to pick image from camera, Gallery
    private String[] camerapermissions;
    private String[] storagePermissions;

    //progress dialog
    private ProgressDialog progressDialog;
    
    //TextRecognizer
    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init UI view
        inputImageBtn = findViewById(R.id.InputImageButton);
        recognizedTextBtn = findViewById(R.id.recognizedTextBtn);
        imageIv = findViewById(R.id.imageIv);
        recognizedTextEt = findViewById(R.id.recognizedTextEt);

        //init arrays of permissions required for camera, gallery
        camerapermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //init setup the progress dialog, showe while text from image is being recognized
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //init textrecognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        
        //handle click, show input image dialog
        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputiImageDialog();
            }
        });

        //handle click, start recognizing text from image we took from camera/gallery
        recognizedTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check if image is picked or not, picked if image uri is not null
                if (imageUri == null){
                    //imageUri is null, which means we haven't picked image yet, can't recognize text
                    Toast.makeText(MainActivity.this, "pick Image first", Toast.LENGTH_SHORT).show();
                } else {
                    //imageUri is not null, which means we have picked image, we can recognize text
                    recognizedTextFromImage();
                }
            }
        });
    }

    private void recognizedTextFromImage() {

        Log.d(TAG, "recognizedTextFromImage: ");
        //set message and show progress dialog
        progressDialog.setMessage("Preparing image....");
        progressDialog.show();

        try {

            //prepare InputImage from imageuri
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);

            //image prepared, we are about to start text recognizition process, change message
            progressDialog.setMessage("Recognizing text....");

            //start text recognition process from image
            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {

                            //process complated, dismiss dialog
                            progressDialog.dismiss();

                            //get the recognized text
                            String recognizedText = text.getText();

                            Log.d(TAG, "onSuccess: recognizedText: "+recognizedText);

                            //set the recognized text to edittext
                            recognizedTextEt.setText(recognizedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            //faild recognized text from image, dismiss dialaog, show reason in toast
                            progressDialog.dismiss();
                            Log.e(TAG, "onFailure: ", e);
                            Toast.makeText(MainActivity.this, "Failed recognizing text due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            
        }catch (Exception e){
            //exception accured while preparing InputImage, dismiss dialog, show reason in toast
            progressDialog.dismiss();
            Log.e(TAG, "recognizedTextFromImage: ", e);
            Toast.makeText(this, "Failed prepairing image due to"+e.getMessage(), Toast.LENGTH_SHORT).show();

        }
    }

    private void showInputiImageDialog() {
        PopupMenu popupMenu = new PopupMenu(this,inputImageBtn);

        //Add items camera, Gallery to Popupmenu, parms 2 is menu id, parms 3 is position of menu item in menu item lists,parms 4 is title of the menu
        popupMenu.getMenu().add(Menu.NONE, 1,1 ,"CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2,2 ,"GALLERY");

        //show popupmenu
        popupMenu.show();

        //handle popupmenu items clicked
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //get item id that is clicked form popupmenu
                int id = menuItem.getItemId();
                if (id==1){
                    //camera is click, check if camera permission are granted or not
                    Log.d(TAG, "onMenuItemClick: Camera Clicked...");
                    if (checkCameraPermission()){
                        //camera permission granted, we can launch camera intent
                        pickImageCamera();
                    }else {
                        //camera permission not granted, request the camera permission
                        requestCameraPermission();
                    }
                }
                else if (id ==2){
                    //storage is click, check if storage permission are granted or not
                    Log.d(TAG, "onMenuItemClick: Gallery Clicked...");
                    if (checkStoragePermission()){
                        //storage permission granted, we can launch storage intent
                        pickImageGallery();
                    }else {
                        //storage permission not granted, request the storage permission
                        requestStoragePermission();
                    }
                }
                return true;
            }
        });
    }

    private void pickImageGallery(){
        Log.d(TAG, "pickImageGallery: ");
        //intent to pick image from gallery, will show all resource form where we can pick the image
        Intent intent = new Intent(Intent.ACTION_PICK);
        //set type of file we want to pick i.e image
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here we will receive the image, if picked
                    if (result.getResultCode() == Activity.RESULT_OK){
                        //image picked
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri: "+imageUri);
                        //set to imageview
                        imageIv.setImageURI(imageUri);
                    }else{
                        //cancelled
                        Log.d(TAG, "onActivityResult: cancelled");
                        Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"sample title");
        values.put(MediaStore.Images.Media.DESCRIPTION,"sample description");
        //image uri
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        //intent to launch camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        camerayActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> camerayActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here we will receive the image, if taken from camera
                    if (result.getResultCode() == Activity.RESULT_OK){
                        //image is taken form camera
                        //we already have the image in imageUri using function pickImageCamera()
                        Log.d(TAG, "onActivityResult: imageUri: "+imageUri);
                        imageIv.setImageURI(imageUri);
                    }else{
                        //cancelled
                        Log.d(TAG, "onActivityResult: cancelled");
                        Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private boolean checkStoragePermission(){
        /* check if storage permission is allowed or not
        return true if allowed, false if not allowed
         */
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        //request storage permission (for gallery image pick)
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }
    private boolean checkCameraPermission(){
        /* check if camera permission is allowed or not
        return true if allowed, false if not allowed
         */
        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return cameraResult && storageResult;
    }

    private void requestCameraPermission(){
        //request camera permission (for camera intent)
        ActivityCompat.requestPermissions(this, camerapermissions,CAMERA_REQUEST_CODE);
    }

    //handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                //check if some action from permission dialog performed or not allowed
                if (grantResults.length>0){
                    //check if camera, storage permission granted, contains boolean results either true or false
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean galleryAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    //check if bath permissions are granted or not
                    if (cameraAccepted && galleryAccepted){
                        //both permissions (camera & gallery) are granted, we can launch camera intent
                        pickImageCamera();
                    }else {
                        //one or both permissions are denied, can't launch camera intent
                        Toast.makeText(this, "Camera & Gallery permissions are required", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    //nether allowed not denied, rather cancelled
                    Toast.makeText(this, "Cancelled...", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                //Check if some actions from permission dialog performed or not allowed/deny
                if(grantResults.length>0){
                    //check if storage permission granted, contains boolean results either true or false
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    //check if storage permission is granted or not
                    if (storageAccepted){
                        //storage permission granted, we can launch gallery intent
                        pickImageGallery();
                    }else{
                        //storage permission denied, can't launch gallery intent
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }
}