package no.nordicsemi.android.blinky.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
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
import java.net.InetAddress;

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
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private NsdServiceInfo mServiceInfo;
    public String mEsp32Address;
    private static final String SERVICE_TYPE = "_http._tcp.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mNsdManager = (NsdManager) (getApplicationContext().getSystemService(Context.NSD_SERVICE));
        }


        initializeResolveListener();
        initializeDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        sharedpreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        button = findViewById(R.id.next);
        spinner = findViewById(R.id.spinner);
        editText = findViewById(R.id.carNumber);
        editText2 = findViewById(R.id.ipAddress);
        if (sharedpreferences.contains("ipAddress")) {
            ipAddress = sharedpreferences.getString("ipAddress", "");
            editText2.setText(ipAddress);
        }
        String[] items = new String[]{"2", "36", "24", "18", "12"};
        selected = "2";
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
                }
                carNumber = editText.getText().toString();

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
                String url = "http://" + ipAddress + "/number/" + selected;
                StringRequest mStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        showMessage(response);
                        File directory = new File(Environment.getExternalStorageDirectory(), "Bot");
                        sharedpreferences.edit().clear().apply();
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

    private void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.i("started disc", "onDiscoveryStarted: ");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                String name = service.getServiceName();
                String type = service.getServiceType();
                Log.d("NSD", "Service Name=" + name);
                Log.d("NSD", "Service Type=" + type);
                if (type.equals(SERVICE_TYPE) && name.contains("esp32")) {
                    Log.d("NSD", "Service Found @ '" + name + "'");
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i("stopped", "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("failed", "Discovery failed: Error code:" + errorCode);

                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("failed", "Discovery failed: Error code:" + errorCode);

                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    private void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e("NSD", "Resolve failed" + errorCode);
                showMessage("Failed to find esp32 " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                mServiceInfo = serviceInfo;

                // Port is being returned as 9. Not needed.
                //int port = mServiceInfo.getPort();

                InetAddress host = mServiceInfo.getHost();
                String address = host.getHostAddress();
                Log.d("NSD", "Resolved address = " + address);
                showMessage("found esp32 at " + address);
                mEsp32Address = address;
            }
        };
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