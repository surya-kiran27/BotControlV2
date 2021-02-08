package no.nordicsemi.android.blinky;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import no.nordicsemi.android.ble.livedata.state.ConnectionState;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.viewmodels.BotViewModel;

public class Remote extends AppCompatActivity {
    public static final String EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE";
    private BotViewModel viewModel;
    private boolean connected;
    private int botState=1;
    private Button botStateButton;
    private Button stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        botStateButton=findViewById(R.id.buttonState);
        stop=findViewById(R.id.stop);
        Intent intent = getIntent();
        DiscoveredBluetoothDevice device=intent.getParcelableExtra(EXTRA_DEVICE);
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

        botStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(botState==0){
                    botState=1;
                    viewModel.setBotState(1);
                    botStateButton.setText("Pause");
                }else{
                    botState=0;

                    viewModel.setBotState(0);
                    botStateButton.setText("Continue");
                }
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(botState==1){
                    botState=0;
                    viewModel.setBotState(0);
                    botStateButton.setText("Continue");
                }else{
                    showToast("Bot already in stop state");
                }
            }
        });
    }
    private void showToast(String errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}