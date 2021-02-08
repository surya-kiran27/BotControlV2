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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.securepreferences.SecurePreferences;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import no.nordicsemi.android.ble.livedata.state.ConnectionState;
import no.nordicsemi.android.blinky.R;
import no.nordicsemi.android.blinky.ScannerActivity;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.profile.data.FileUploader;
import no.nordicsemi.android.blinky.viewmodels.BotViewModel;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

public class TakePicture extends AppCompatActivity {
    public static final String EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE";

    private BotViewModel viewModel;
    private  boolean connected;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private int REQUEST_CODE_PERMISSIONS = 101;
    private String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"};
    private String carNumber;
    private int noImage;
    private float zoomLevel;
    private int clickState=0;
    private int noImages=0;
    private int imageCounter=1;
    private  int botState=1;
    private  int prevBotState=1;
    private int stop=0;
    PreviewView previewView;
    ImageCapture imageCapture;
    Executor cameraExecutor;
    SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);
        Intent intent = getIntent();
        DiscoveredBluetoothDevice device=intent.getParcelableExtra(EXTRA_DEVICE);
        sharedPreferences= getSharedPreferences("settings", Context.MODE_PRIVATE);
        carNumber =sharedPreferences.getString("carNumber","");
        noImage =  sharedPreferences.getInt("noImage",0);

        Log.i("zoomLevel", "onCreate: "+zoomLevel);
        viewModel = new ViewModelProvider(this).get(BotViewModel.class);
        viewModel.connect(device);
        viewModel.getConnectionState().observe(this, new Observer<ConnectionState>() {
            @Override
            public void onChanged(ConnectionState state) {
                switch (state.getState()) {
                    case CONNECTING:
                        showToast("Connecting....");
                        break;
                    case READY: {
                        showToast("Connected");
                        connected=true;

                        viewModel.setNoImages(noImage);
                        viewModel.setBotState(1);
                        viewModel.setClick();
                        break;
                    }
                    case DISCONNECTED:
                        if (state instanceof ConnectionState.Disconnected) {
                            final ConnectionState.Disconnected stateWithReason = (ConnectionState.Disconnected) state;
                            if (stateWithReason.isNotSupported()) {
                                showToast("Device not supported");

                            }else{
                                showToast("Disconnected");
                            }
                            connected=false;
                        }
                        // fallthrough
                    case DISCONNECTING:
                        showToast("Disconnecting..");
                        break;
                }
            }

        });
        viewModel.getClickState().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                Log.i("click state changed", "onChanged: "+integer+" "+botState);
                clickState=integer;
                if (clickState==1&&botState==1){
                    onClick();
                }
            }


        });
        viewModel.getNoImages().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
//                Log.i("images number changed", "onChanged: "+integer);
                noImages=integer;
            }


        });
        viewModel.getBotState().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
           if(integer==1){
               botState=1;
               prevBotState=0;
           }else if(integer==0&&prevBotState==0){
                prevBotState=1;
                botState=0;
           }

           if(prevBotState==1&&botState==0&&stop==0){
               Log.i("close state", "onChanged: "+"error");
               stop=1;
               uploadMultiFile();

           }

            }
        });

        cameraExecutor = ContextCompat.getMainExecutor(this);
        previewView = (PreviewView) findViewById(R.id.previewView);
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
    }

   private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        imageCapture =
               new ImageCapture.Builder()
                       .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                       .build();
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector,imageCapture, preview);
     CameraControl cameraControl=camera.getCameraControl();
     zoomLevel = sharedPreferences.getFloat("zoomLevel",0f);

       Log.i("mod zoom level", "bindPreview: "+zoomLevel/100.0f);
     cameraControl.setLinearZoom(zoomLevel/100.0f);
    }
    private void showToast(String errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
    public void onClick() {
        String FileName=imageCounter+".jpg";
        Log.i("image save", "called: ");
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"Bot");

        boolean success = true;
        if (!directory.exists()) {
            success = directory.mkdir();
        }
        if (success) {

            ImageCapture.OutputFileOptions outputFileOptions =
                    new ImageCapture.OutputFileOptions.Builder(new File(directory,FileName)).build();
            imageCapture.takePicture(outputFileOptions, cameraExecutor,
                    new ImageCapture.OnImageSavedCallback () {
                        @Override
                        public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                            // insert your code here.
                            showToast("image saved");
                            imageCounter+=1;
                            viewModel.setClick();
                        }
                        @Override
                        public void onError(ImageCaptureException error) {
                            error.printStackTrace();
                            showToast("Failed to  save");
                        }
                    });
        } else {
            // Do something else on failure
            showToast("Could not create folder");

        }

    }

    private void uploadMultiFile() {
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"Bot");
        if(folder.exists()){
            showToast("Project is being created...");
            SharedPreferences prefs = new SecurePreferences(getApplicationContext());
            String auth_token="Bearer ";
            String token= prefs.getString("api_token","");
            String registration=sharedPreferences.getString("carNumber","");
            auth_token+=token;
            File[] filesToUpload=folder.listFiles();
            if(filesToUpload!=null&&filesToUpload.length>0){
                Log.i("bot value changed", "files upload: "+filesToUpload.length);

                FileUploader fileUploader = new FileUploader();
                SharedPreferences sharedpreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
                if (sharedpreferences.contains("id")){
                    int id=sharedPreferences.getInt("id",0);
                    Log.i("update project", "uploadMultiFile: "+id);
                    fileUploader.updateFiles("https://dev.onetobeam.com/spinomate/public/api/projects/"+id, "images[]", filesToUpload, new FileUploader.FileUploaderCallback() {
                        @Override
                        public void onError() {
                            showToast("Failed to upload");

                        }

                        @Override
                        public void onFinish(String responses) {
                            showToast("Project created!");
                            Intent i = new Intent(getApplicationContext(), Projects.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getApplication().startActivity(i);
                            finish();
                        }

                    },auth_token,registration);
                }else{
                    fileUploader.uploadFiles("https://dev.onetobeam.com/spinomate/public/api/projects", "images[]", filesToUpload, new FileUploader.FileUploaderCallback() {
                        @Override
                        public void onError() {
                            showToast("Failed to upload");

                        }

                        @Override
                        public void onFinish(String responses) {
                            showToast("Project created!");
                            Intent i = new Intent(getApplicationContext(), Projects.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getApplication().startActivity(i);
                            finish();
                        }

                    },auth_token,registration);
                }

            }
        }


    }
    // And override this method
}