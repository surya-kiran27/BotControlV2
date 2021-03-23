package no.nordicsemi.android.blinky.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.common.util.concurrent.ListenableFuture;
import com.securepreferences.SecurePreferences;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import no.nordicsemi.android.blinky.R;
import no.nordicsemi.android.blinky.RequestSingleton;
import no.nordicsemi.android.blinky.profile.data.FileUploader;

public class TakePicture extends AppCompatActivity {
    PreviewView previewView;
    ImageCapture imageCapture;
    Executor cameraExecutor;
    SharedPreferences sharedPreferences;
    String clickUrl;
    String stateUrl;
    Button stopBot;
    Timer timer;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private String carNumber;
    private String ipAddress;
    private int noImage;
    private float zoomLevel;
    private int clickState = 0;
    private int noImages = 0;
    private int imageCounter = 1;
    private int botState = 1;
    private int prevBotState = 1;
    private boolean clicking = false;
    private int stop = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);
        stopBot = findViewById(R.id.stopButton);
        sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        carNumber = sharedPreferences.getString("carNumber", "");
        ipAddress = sharedPreferences.getString("ipAddress", "");
        Log.i("ipAddress", "onCreate: " + ipAddress);
        noImages = sharedPreferences.getInt("noImage", 0);
        clickUrl = "http://" + ipAddress + "/click/";
        stateUrl = "http://" + ipAddress + "/start/";
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                StringRequest mStringRequest = new StringRequest(Request.Method.GET, "http://" + ipAddress + "/both/", response -> {
                    int clickVal = Integer.parseInt(response.split(",")[0].trim());
                    int botVal = Integer.parseInt(response.split(",")[1].trim());

                    clickState = clickVal;
                    Log.i("clickState", "click state changed to : " + clickState);
                    if (clickState == 1 && botState == 1 && !clicking) {
                        clicking = true;
                        onClick();
                    }
                    Log.i("botState", "bot State received: " + botVal);
                    if (botVal == 1) {
                        botState = 1;
                        prevBotState = 0;
                    } else if (botVal == 0 && prevBotState == 0) {
                        prevBotState = 1;
                        botState = 0;
                    }
                    if (prevBotState == 1 && botState == 0 && stop == 0) {
                        Log.i("close state", "onChanged: " + "error");
                        stop = 1;
//                        uploadMultiFile();
                    }
                    Log.i("botState", "bot State changed to : " + botState);
                }, error -> Log.i("bothVals", "Failed to poll : " + error.getMessage()));
                RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(mStringRequest);
            }
        };


        StringRequest mStringRequest2Local = new StringRequest(Request.Method.GET, stateUrl.concat("1"), response -> {
            botState = 1;
            timer.schedule(task, 0, 500); //time in ms
            showToast("Bot Start State initialized to " + botState);
            Log.i("botState", "bot state initialized to : " + botState);
        }, error -> {
            showToast("Could not send initial start state data to Bot");
            Log.i("botState", "onCreate: Failed to send state " + error.getMessage() + stateUrl);
        });
        StringRequest mStringRequestLocal = new StringRequest(Request.Method.GET, clickUrl.concat("0"), response -> {
            clickState = 0;
            RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(mStringRequest2Local);
            showToast("Bot Click State initialized to " + botState);

            Log.i("clickState", "click state initialized to : " + clickState);
        }, error -> showToast("Could not send initial click data to Bot" + error.getMessage() + clickUrl));

        RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(mStringRequestLocal);


        cameraExecutor = ContextCompat.getMainExecutor(this);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, cameraExecutor);

        stopBot.setOnClickListener(v -> {
            StringRequest mStringRequest = new StringRequest(Request.Method.GET, stateUrl.concat("0"), response2 -> {
                showToast("Bot state changed to stop!");
                Intent i = new Intent(getApplicationContext(), Projects.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(i);
                finish();
//                uploadMultiFile();
            }, error -> {
                showToast("Failed to send stop command to bot..Try Again!" + error.getMessage());
            });
            RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(mStringRequest);
        });
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().setTargetRotation(this.getResources().getConfiguration().orientation)
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        imageCapture =
                new ImageCapture.Builder()
                        .setTargetRotation(this.getResources().getConfiguration().orientation)
                        .build();
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, preview);
        CameraControl cameraControl = camera.getCameraControl();
        zoomLevel = sharedPreferences.getFloat("zoomLevel", 0f);

        Log.i("mod zoom level", "bindPreview: " + zoomLevel / 100.0f);
        cameraControl.setLinearZoom(zoomLevel / 100.0f);
    }

    private void showToast(String errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    public void onClick() {
        String FileName = imageCounter + ".jpg";
        Log.i("image save", "called: ");
        File directory = new File(Environment.getExternalStorageDirectory(), "Bot");
        boolean success = true;
        if (!directory.exists()) {
            success = directory.mkdirs();
        }
        if (success) {
            ImageCapture.OutputFileOptions outputFileOptions =
                    new ImageCapture.OutputFileOptions.Builder(new File(directory, FileName)).build();
            imageCapture.takePicture(outputFileOptions, cameraExecutor,
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                            // insert your code here.
                            StringRequest mStringRequest = new StringRequest(Request.Method.GET, clickUrl.concat("0"), response -> {
                                clickState = 0;
                                Log.i("clickState", "click state changed to : " + clickState);
                                clicking = false;
                                showToast("image " + imageCounter + " saved");
                                if (imageCounter >= noImages) {
                                    showToast("Finished clicking images");

                                }
                                imageCounter += 1;

                            }, error -> {
                                showToast("Could not send click command to Bot" + error.getMessage());
                                clicking = false;
                            });
                            RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(mStringRequest);
                        }

                        @Override
                        public void onError(ImageCaptureException error) {
                            error.printStackTrace();
                            clicking = false;
                            showToast("Failed to  save" + error.getMessage());
                        }
                    });
        } else {
            // Do something else on failure
            clicking = false;
            showToast("Could not create folder");

        }

    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }

        super.onDestroy();
    }

    private void uploadMultiFile() {
        File folder = new File(Environment.getExternalStorageDirectory(), "Bot");
        if (folder.exists()) {
            showToast("Project is being created...");
            SharedPreferences prefs = new SecurePreferences(getApplicationContext());
            String auth_token = "Bearer ";
            String token = prefs.getString("api_token", "");
            String registration = sharedPreferences.getString("carNumber", "");
            auth_token += token;
            File[] filesToUpload = folder.listFiles();
            if (filesToUpload != null && filesToUpload.length > 0) {
                Log.i("bot value changed", "files upload: " + filesToUpload.length);
                showToast("Uploading...");
                FileUploader fileUploader = new FileUploader();
                SharedPreferences sharedpreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);

                fileUploader.uploadFiles("https://dev.onetobeam.com/spinomate/public/api/projects", "images[]", filesToUpload, new FileUploader.FileUploaderCallback() {
                    @Override
                    public void onError() {
                        showToast("Failed to upload");
                        Intent i = new Intent(getApplicationContext(), Projects.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(i);
                        finish();
                    }

                    @Override
                    public void onFinish(String responses) {
                        if (timer != null) {
                            timer.cancel();
                        }
                        if (responses == null) {
                            showToast("Project Failed to create!" + responses);
                        } else {
                            showToast("Project created!" + responses);
                            sharedpreferences.edit().clear().apply();

                        }

                        Log.i("Res", "onFinish: " + responses);
                        Intent i = new Intent(getApplicationContext(), Projects.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(i);
                        finish();
                    }

                }, auth_token, registration);


            }
        }


    }
    // And override this method
}