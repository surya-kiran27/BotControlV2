package no.nordicsemi.android.blinky.profile.callback;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.callback.DataSentCallback;
import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;

public abstract class BotState implements DataSentCallback, ProfileDataCallback, BotStateCallback {
    @Override
    public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {

        if (data.size() != 1) {
            onInvalidDataReceived(device, data);
            return;
        }


        final int state = Integer.parseInt(data.getStringValue( 0));
//        Log.i("bot value received", "onDataReceived: "+data.getStringValue(0));
        if(state==0||state==1)
            onBotStateChanged(device,state);
        else
            onInvalidDataReceived(device, data);

    }
    @Override
    public void onDataSent(@NonNull BluetoothDevice device, @NonNull Data data) {
        parse(device, data);
    }
    private void parse(@NonNull final BluetoothDevice device, @NonNull final Data data) {

        if (data.size() != 1) {
            onInvalidDataReceived(device, data);
            return;
        }

        final int no = data.getIntValue(Data.FORMAT_UINT8, 0);
//        Log.i("click value sent", "onDataReceived: "+data.getIntValue(Data.FORMAT_UINT8,0));


        onBotStateChanged(device, no);

    }
}