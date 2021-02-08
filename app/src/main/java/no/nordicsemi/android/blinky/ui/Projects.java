package no.nordicsemi.android.blinky.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.ContextWrapper;
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
import java.io.IOException;
import java.util.ArrayList;


import no.nordicsemi.android.blinky.R;
import no.nordicsemi.android.blinky.ScannerActivity;

public class Projects extends AppCompatActivity {
    private ListView listView;
    private Button button;
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"Bot");
//        deleteFiles(directory.getAbsolutePath());
        sharedpreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        sharedpreferences.edit().clear().apply();
        listView = (ListView) findViewById(R.id.list);
        button=findViewById(R.id.button);
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(itemsAdapter);
        ArrayList<Integer> ids=new ArrayList<>();
        String url = "https://dev.onetobeam.com/spinomate/public/api/projects";
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        try {
            JSONObject object = new JSONObject();
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        JSONArray array=response.getJSONArray("data");
                        Log.i("projects", "onResponse: "+array);
                        if (array.length()<=10){
                            for (int i = 0; i<10 ; i++) {
                                JSONObject jsonObject= array.getJSONObject(i);
                                itemsAdapter.add(jsonObject.getString("registration_no"));
                                ids.add(jsonObject.getInt("id"));
                            }
                        }else{
                            for (int i = 0; i<10 ; i++) {
                                JSONObject jsonObject= array.getJSONObject((array.length()-1)-i);
                                itemsAdapter.add(jsonObject.getString("registration_no"));
                                ids.add(jsonObject.getInt("id"));
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
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                Log.i("selected option", "onItemClick: "+selectedItem+ids.get(position));
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("carNumber",selectedItem);
                editor.putInt("id",ids.get(position));
                editor.apply();
                Intent i = new Intent(getApplicationContext(), CreateProject.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(i);
                finish();
            }
        });
    }
    public static void deleteFiles(String path) {

        File file = new File(path);

        if (file.exists()) {
            String deleteCmd = "rm -r " + path;
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
            } catch (IOException e) { }
        }
    }

}

