package no.nordicsemi.android.blinky.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.securepreferences.SecurePreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;


import no.nordicsemi.android.blinky.ProjectInfo;
import no.nordicsemi.android.blinky.R;
import no.nordicsemi.android.blinky.profile.data.FileUploader;

public class Projects extends AppCompatActivity {
    private ListView listView;
    private Button button;
    private Button refresh;
    ArrayList<JSONObject> ids;
    SharedPreferences sharedpreferences;
    ArrayAdapter<String> itemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);
        sharedpreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        listView = findViewById(R.id.list);
        button = findViewById(R.id.button);
        refresh = findViewById(R.id.refresh);
        itemsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(itemsAdapter);
        ids = new ArrayList<>();

        SharedPreferences prefs = new SecurePreferences(getApplicationContext());
        String auth_token = "Bearer ";
        String token = prefs.getString("api_token", "");
        auth_token += token;
        File folder = new File(Environment.getExternalStorageDirectory(), "Bot");
        File[] filesToUpload = folder.listFiles();
        FileUploader fileUploader = new FileUploader();
        Log.i("attempting to upload", "onCreate: " + sharedpreferences.contains("carNumber"));
        if (filesToUpload != null && filesToUpload.length > 0 && sharedpreferences.contains("carNumber")) {

            String registration = sharedpreferences.getString("carNumber", "");
            Log.i("attempting to upload", "onCreate: " + registration + " " + filesToUpload.length);

            showToast("Project uploading....." + registration);
            fileUploader.uploadFiles("https://dev.onetobeam.com/spinomate/public/api/projects", "images[]", filesToUpload, new FileUploader.FileUploaderCallback() {
                @Override
                public void onError() {
                    showToast("Failed to upload");

                }

                @Override
                public void onFinish(String responses) {
                    showToast("Project upload status" + responses);
                }

            }, auth_token, registration);
        }
        getProjects();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //create project
                Intent i = new Intent(getApplicationContext(), CreateProject.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(i);
                finish();
            }
        });
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //create project
                itemsAdapter.clear();
                ids.clear();
                getProjects();
                showToast("Updated projects");
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                JSONObject jsonObject = ids.get(position);

                Log.i("selected option", "onItemClick: " + selectedItem + ids.get(position));
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("carNumber", selectedItem);
                try {
                    editor.putInt("id", jsonObject.getInt("id"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                editor.apply();
                Intent i = new Intent(getApplicationContext(), ProjectInfo.class);
                try {
                    i.putExtra("url", jsonObject.getString("product_view_url"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(i);
                finish();
            }
        });
    }

    private void showToast(String errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    private void getProjects() {

        String url = "https://dev.onetobeam.com/spinomate/public/api/projects";
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        try {
            JSONObject object = new JSONObject();
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        JSONArray array = response.getJSONArray("data");
                        Log.i("projects", "onResponse: "+array);
                        if (array.length()<=10){
                            for (int i = 0; i<10 ; i++) {
                                JSONObject jsonObject= array.getJSONObject(i);
                                itemsAdapter.add(jsonObject.getString("registration_no"));
                                ids.add(jsonObject);
                            }
                        }else{
                            for (int i = 0; i<10 ; i++) {
                                JSONObject jsonObject= array.getJSONObject((array.length()-1)-i);
                                itemsAdapter.add(jsonObject.getString("registration_no"));
                                ids.add(jsonObject);
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
                }
            });
            requestQueue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

