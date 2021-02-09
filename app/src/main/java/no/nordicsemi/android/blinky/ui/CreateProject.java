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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import no.nordicsemi.android.blinky.R;
import no.nordicsemi.android.blinky.RequestSingleton;

import static java.lang.Integer.parseInt;

public class CreateProject extends AppCompatActivity {
    private Button button;
    private Spinner spinner;
    private EditText editText;
    private EditText editText2;
    private String selected;
    ArrayAdapter<String> adapter;
    SharedPreferences sharedpreferences;
    String ipAddress = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);
        sharedpreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        button = findViewById(R.id.next);
        spinner = findViewById(R.id.spinner);
        editText = findViewById(R.id.carNumber);
        editText2 = findViewById(R.id.ipAddress);
        if (sharedpreferences.contains("ipAddress")) {
            ipAddress = sharedpreferences.getString("ipAddress", "");
            editText2.setText(ipAddress);
        }
        String[] items = new String[]{"36", "24", "18", "12"};
        selected = "36";
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                selected = adapter.getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here

            }

        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String carNumber = "";

                if (sharedpreferences.contains("carNumber")) {
                    carNumber = sharedpreferences.getString("carNumber", "");
                } else {
                    carNumber = editText.getText().toString();
                }

                if (carNumber.length() == 0) {
                    showMessage("Please enter a valid car number");
                    return;
                }

                ipAddress = String.valueOf(editText2.getText());
                if (ipAddress.length() == 0) {
                    showMessage("Please enter a valid IP Address");
                    return;
                }
                String finalCarNumber = carNumber;
                String url = "http://" + ipAddress + "/number/" + carNumber;
                StringRequest mStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        showMessage(response);
                        File directory = new File(Environment.getExternalStorageDirectory(), "Bot");
                        deleteFiles(directory.getPath());
                        Intent i = new Intent(getApplicationContext(), CameraSettings.class);
                        i.putExtra("carNumber", finalCarNumber);
                        i.putExtra("noImage", selected);
                        i.putExtra("ipAddress", ipAddress);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(i);
                        finish();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("noImages", "onErrorResponse: Failed to send no images " + error.getMessage() + url);
                        showMessage("Could not send data to Bot");
                    }
                });

                RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(mStringRequest);

            }
        });
    }

    private void showMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public static void deleteFiles(String path) {

        File file = new File(path);

        if (file.exists()) {
            String deleteCmd = "rm -r " + path;
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
            } catch (IOException e) {
            }
        }
    }

}