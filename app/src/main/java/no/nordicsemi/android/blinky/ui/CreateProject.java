package no.nordicsemi.android.blinky.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
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
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;

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
    NsdManager.DiscoveryListener mDiscoveryListener;
    String mServiceName;
    NsdServiceInfo mServiceInfo;
    ServerSocket mServerSocket;
    int mLocalPort;

    NsdManager mNsdManager;

    final String TAG = "---Networking";
    final String SERVICE_TYPE = "_http._tcp.";

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
        String[] items = new String[]{"4", "36", "24", "18", "12"};
        selected = "4";
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


                if (editText2.getText().length() > 0) {
                    Log.i(TAG, "onClick: " + editText2.getText().toString());
                    ipAddress = editText2.getText().toString();
                }
                if (ipAddress.length() == 0) {
                    showMessage("Invalid ip address");
                    return;
                }
                String finalCarNumber = carNumber;
                String url = "http://" + ipAddress + "/number/" + selected;
                StringRequest mStringRequest = new StringRequest(Request.Method.GET, url, response -> {

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
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("noImages", "onErrorResponse: Failed to send no images " + error.getMessage() + url);
                        showMessage("Could not send data to Bot" + error.getMessage());
                    }
                });

                RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(mStringRequest);

            }
        });
        try {
            mServerSocket = new ServerSocket(0);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Store the chosen port.
//        mLocalPort = mServerSocket.getLocalPort();

//        registerService(mLocalPort);
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

    public void registerService(int port) {
        // Create the NsdServiceInfo object, and populate it.
        mServiceInfo = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        mServiceInfo.setServiceName("NsdChat");
        mServiceInfo.setServiceType("_workstation._tcp.");
        mServiceInfo.setPort(port);

        mNsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);

        initializeDiscoveryListener();
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }


    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
                showMessage("Searching for esp32.....");

            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success: " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains("esp32")) {
                    mNsdManager.resolveService(service, new NsdManager.ResolveListener() {

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            // TODO Auto-generated method stub
                            mServiceInfo = serviceInfo;

                            // Port is being returned as 9. Not needed.
                            //int port = mServiceInfo.getPort();

                            InetAddress host = mServiceInfo.getHost();
//                            ipAddress = host.getHostAddress();
//                            showMessage("esp32 found at " + ipAddress);
                            Log.d("NSD", "Resolved address = " + ipAddress);
                        }

                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            // TODO Auto-generated method stub
                            Log.d(TAG, "Service resolve failed!");
//                            showMessage("failed to find esp32");

                        }

                    });
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }


}