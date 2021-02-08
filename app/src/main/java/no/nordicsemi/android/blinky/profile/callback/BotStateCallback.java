package no.nordicsemi.android.blinky.profile.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

public interface BotStateCallback {

    void onBotStateChanged(@NonNull final BluetoothDevice device, final int click);
}
