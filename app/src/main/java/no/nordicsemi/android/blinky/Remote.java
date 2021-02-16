package no.nordicsemi.android.blinky;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import java.util.Timer;
import java.util.TimerTask;

public class Remote extends AppCompatActivity {
    private int botState = 1;
    private Button botStateButton;
    private Button stop;
    private EditText editText;
    private String stateUrl;
    private Timer timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        botStateButton = findViewById(R.id.buttonState);
        stop = findViewById(R.id.stop);
        editText = findViewById(R.id.ipAddress2);
        stateUrl = "http://" + editText.getText() + "/start/";
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
        editText.addTextChangedListener(new TextWatcher() {

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
                    stateUrl = "http://" + s + "/start/";
            }
        });
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
}