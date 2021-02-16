package no.nordicsemi.android.blinky.profile.data;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonElement;
import com.securepreferences.SecurePreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Url;

public class FileUploader {

    public FileUploaderCallback fileUploaderCallback;
    private File[] files;
    public int uploadIndex = -1;
    private String uploadURL = "";
    private String filekey = "";
    private final UploadInterface uploadInterface;
    private String auth_token = "";
    private String responses;
    private String registration_no;

    private interface UploadInterface {

        @Multipart
        @POST
        Call<JsonElement> uploadFile(@Url String url, @Part MultipartBody.Part[] images, @Part MultipartBody.Part registration_no, @Header("Authorization") String authorization);

        @Multipart
        @POST
        Call<JsonElement> uploadFile(@Url String url, @Part MultipartBody.Part[] images);

        @Multipart
        @PUT
        Call<JsonElement> updateFiles(@Url String url, @Part MultipartBody.Part[] images, @Part MultipartBody.Part registration_no, @Header("Authorization") String authorization);
    }

    public interface FileUploaderCallback{
        void onError();
        void onFinish(String responses);
    }



    public FileUploader(){
        uploadInterface = ApiClient.getClient().create(UploadInterface.class);
    }


    public void uploadFiles(String url,String filekey,File[] files, FileUploaderCallback fileUploaderCallback,String auth_token,String registration_no){
        this.fileUploaderCallback = fileUploaderCallback;
        this.files = files;
        this.uploadIndex = -1;
        this.uploadURL = url;
        this.filekey = filekey;
        this.auth_token = auth_token;
        this.registration_no=registration_no;
        Log.i("bot value changed", "onChanged:check1 ");
        MultipartBody.Part[] images = new MultipartBody.Part[files.length];
        MultipartBody.Part registration_noPart = MultipartBody.Part.createFormData("registration_no", registration_no);

        for(int i=0; i<files.length; i++){
            File file = new File(files[i].getPath());
            Log.i("bot value changed", "onChanged:check2 ");

            RequestBody data =  RequestBody.create(MediaType.parse("multipart/form-data"),
                    file);

            images[i] = MultipartBody.Part.createFormData("images[]",
                    file.getName(),
                    data);
        }

        Call<JsonElement> call;
        if(auth_token.isEmpty()){
            call  = uploadInterface.uploadFile(uploadURL, images);
        }else{
            call  = uploadInterface.uploadFile(uploadURL, images,registration_noPart, auth_token);
        }

        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, retrofit2.Response<JsonElement> response) {
                if (response.isSuccessful()) {
                    JsonElement jsonElement = response.body();
                    assert jsonElement != null;
                    Log.i("bot value changed", "onChanged: "+jsonElement.toString());
                    responses=jsonElement.toString();
                }else{
                    Log.i("bot value changed", "onChanged: " + response.toString());
                    responses = null;
                }

            }
            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                fileUploaderCallback.onError();
            }
        });
        fileUploaderCallback.onFinish(responses);

    }

    public void updateFiles(String url,String filekey,File[] files, FileUploaderCallback fileUploaderCallback,String auth_token,String registration_no){
        this.fileUploaderCallback = fileUploaderCallback;
        this.files = files;
        this.uploadIndex = -1;
        this.uploadURL = url;
        this.filekey = filekey;
        this.auth_token = auth_token;
        this.registration_no=registration_no;
        Log.i("bot value changed", "onChanged:check1 inside update files "+files.length);
        MultipartBody.Part[] images = new MultipartBody.Part[files.length];
        MultipartBody.Part registration_noPart = MultipartBody.Part.createFormData("registration_no", registration_no);

        for(int i=0; i<files.length; i++){
            File file = new File(files[i].getPath());
            RequestBody data =  RequestBody.create(MediaType.parse("multipart/form-data"),
                    file);

            images[i] = MultipartBody.Part.createFormData("images[]",
                    file.getName(),
                    data);
        }

        Call<JsonElement> call;
        call  = uploadInterface.updateFiles(uploadURL, images,registration_noPart, auth_token);
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, retrofit2.Response<JsonElement> response) {
                if (response.isSuccessful()) {
                    JsonElement jsonElement = response.body();
                    assert jsonElement != null;
                    Log.i("bot value changed", "onChanged: "+jsonElement.toString());
                    responses=jsonElement.toString();
                }else{
                    Log.i("bot value changed", "onChanged: "+response.toString());
                }

            }
            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                t.printStackTrace();
                Log.i("failed to update", "onFailure:"+t.getMessage());
                fileUploaderCallback.onError();
            }
        });
        fileUploaderCallback.onFinish(responses);

    }

}