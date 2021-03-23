package no.nordicsemi.android.blinky;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Timer;
import java.util.TimerTask;

public class Remote extends AppCompatActivity {
    private int botState = 1;
    String ipAddress = "";
    private Button botStateButton;
    private Button stop;
    private EditText ipAddressField;
    private String stateUrl;
    private Timer timer;
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
        setContentView(R.layout.activity_remote);
        botStateButton = findViewById(R.id.buttonState);
        ipAddressField = findViewById(R.id.ipAddressRemote);
        stop = findViewById(R.id.stop);
        stateUrl = "http://" + ipAddress + "/start/";
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                StringRequest mStringRequest2 = new StringRequest(Request.Method.GET, stateUrl, response -> {
                    botState = Integer.parseInt(response.split(":")[1].trim());
                    Log.i("botState", "bot State changed to : " + botState);
                }, error -> Log.i("botState", "Failed to poll : " + stateUrl + error.getMessage()));
                RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(mStringRequest2);
            }
        };
        timer.schedule(task, 0, 1500); // 60000 is time in ms
        botStateButton.setOnClickListener(v -> {
            if (botState == 0) {
                setBotStateToContinue();
            } else {
                setBotStateToPause();
            }
        });
        stop.setOnClickListener(v -> {
            if (botState == 1) {
                setBotStateToPause();
            } else {
                showToast("Bot already in stop state");
            }
        });
        ipAddressField.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if (s.length() != 0)
                    ipAddress = s.toString();
            }
        });
//        try {
//            mServerSocket = new ServerSocket(0);
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        // Store the chosen port.
//        mLocalPort = mServerSocket.getLocalPort();

//        registerService(mLocalPort);
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }

        super.onDestroy();
    }

    private void setBotStateToContinue() {
        StringRequest mStringRequest = new StringRequest(Request.Method.GET, stateUrl + "1", response -> {
            botState = Integer.parseInt(response.split(":")[1].trim());
            botStateButton.setText("Pause");
            Log.i("botState", "bot State changed to : " + botState);
        }, error -> showToast("Failed to change bot state"));
        RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(mStringRequest);
        botStateButton.setText("Pause");
    }

    private void setBotStateToPause() {
        StringRequest mStringRequest = new StringRequest(Request.Method.GET, stateUrl + "0", response -> {
            botState = Integer.parseInt(response.split(":")[1].trim());
            botStateButton.setText("Continue");
            Log.i("botState", "bot State changed to : " + botState);
        }, error -> showToast("Failed to change bot state"));
        RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(mStringRequest);
    }

    private void showToast(String errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

//    public void registerService(int port) {
//        // Create the NsdServiceInfo object, and populate it.
//        mServiceInfo = new NsdServiceInfo();
//
//        // The name is subject to change based on conflicts
//        // with other services advertised on the same network.
//        mServiceInfo.setServiceName("NsdChat");
//        mServiceInfo.setServiceType("_workstation._tcp.");
//        mServiceInfo.setPort(port);
//
//        mNsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);
//
//        initializeDiscoveryListener();
//        mNsdManager.discoverServices(
//                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
//    }
//
//
//    public void initializeDiscoveryListener() {
//
//        // Instantiate a new DiscoveryListener
//        mDiscoveryListener = new NsdManager.DiscoveryListener() {
//
//            //  Called as soon as service discovery begins.
//            @Override
//            public void onDiscoveryStarted(String regType) {
//                Log.d(TAG, "Service discovery started");
//                showToast("Searching for esp32.....");
//
//            }
//
//            @Override
//            public void onServiceFound(NsdServiceInfo service) {
//                // A service was found!  Do something with it.
//                Log.d(TAG, "Service discovery success: " + service);
//                if (!service.getServiceType().equals(SERVICE_TYPE)) {
//                    // Service type is the string containing the protocol and
//                    // transport layer for this service.
//                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
//                } else if (service.getServiceName().equals(mServiceName)) {
//                    // The name of the service tells the user what they'd be
//                    // connecting to. It could be "Bob's Chat App".
//                    Log.d(TAG, "Same machine: " + mServiceName);
//                } else if (service.getServiceName().contains("esp32")) {
//                    mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
//
//                        @Override
//                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
//                            // TODO Auto-generated method stub
//                            mServiceInfo = serviceInfo;
//
//                            // Port is being returned as 9. Not needed.
//                            //int port = mServiceInfo.getPort();
//
//                            InetAddress host = mServiceInfo.getHost();
//                            ipAddress = host.getHostAddress();
//                            stateUrl = "http://" + ipAddress + "/start/";
//                            showToast("esp32 found at " + ipAddress);
//                            Log.d("NSD", "Resolved address = " + ipAddress);
//                        }
//
//                        @Override
//                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
//                            // TODO Auto-generated method stub
//                            Log.d(TAG, "Service resolve failed!");
//                            showToast("failed to find esp32");
//
//                        }
//
//                    });
//                }
//            }
//
//            @Override
//            public void onServiceLost(NsdServiceInfo service) {
//                // When the network service is no longer available.
//                // Internal bookkeeping code goes here.
//                Log.e(TAG, "service lost: " + service);
//            }
//
//            @Override
//            public void onDiscoveryStopped(String serviceType) {
//                Log.i(TAG, "Discovery stopped: " + serviceType);
//            }
//
//            @Override
//            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
//                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
//                mNsdManager.stopServiceDiscovery(this);
//            }
//
//            @Override
//            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
//                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
//                mNsdManager.stopServiceDiscovery(this);
//            }
//        };
//    }
}