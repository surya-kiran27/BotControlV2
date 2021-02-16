package no.nordicsemi.android.blinky.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.securepreferences.SecurePreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import no.nordicsemi.android.blinky.R;
import no.nordicsemi.android.blinky.Remote;
import no.nordicsemi.android.blinky.ui.Projects;

public class LoginActivity extends AppCompatActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText usernameEditText = findViewById(R.id.username);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final Button remoteButton = findViewById(R.id.remote);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //do login here
                String email=usernameEditText.getText().toString();
                String password=passwordEditText.getText().toString();
                Log.i("login clicked", "onClick: "+email+password);
                if(email.length()==0){
                    showLoginFailed("Email cannot be empty");
                    return;
                }
                if(password.length()==0){
                    showLoginFailed("Password cannot be empty");
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                String url = "https://dev.onetobeam.com/spinomate/public/api/login";

                Map<String, String> params = new HashMap();
                params.put("email", email);
                params.put("password", password);

                JSONObject parameters = new JSONObject(params);

                JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //TODO: handle success
                        try {
                            JSONObject data=response.getJSONObject("data");
                            String api_token=data.getString("api_token");
                            String id=data.getString("id");
                            SharedPreferences prefs = new SecurePreferences(getApplicationContext());
                            prefs.edit().putString("api_token",api_token).apply();
                            prefs.edit().putString("email",email).apply();

                            Log.i("response", "onResponse: "+data);
                            Intent i = new Intent(getApplicationContext(), Projects.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getApplication().startActivity(i);
                            finish();
                        } catch (JSONException e) {
                            showLoginFailed("Email or password is incorrect");
                            return;
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        showLoginFailed("Email or password is incorrect");
                    }
                });

                Volley.newRequestQueue(getApplicationContext()).add(jsonRequest);
            }
        });
        remoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = new SecurePreferences(getApplicationContext());
                prefs.edit().clear().apply();
                Intent i = new Intent(getApplicationContext(), Remote.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getApplication().startActivity(i);
                            finish();
            }
        });
    }



    private void showLoginFailed(String errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}