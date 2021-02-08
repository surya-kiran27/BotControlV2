/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.blinky.viewmodels;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import no.nordicsemi.android.ble.livedata.state.ConnectionState;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.profile.BotManager;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

public class BotViewModel extends AndroidViewModel {
	private final BotManager botManager;
	private BluetoothDevice device;

	public BotViewModel(@NonNull final Application application) {
		super(application);

		// Initialize the manager.
		botManager = new BotManager(getApplication());
	}

	public LiveData<ConnectionState> getConnectionState() {
		return botManager.getState();
	}

	public LiveData<Integer> getClickState() {
		return botManager.getClickState();
	}

	public LiveData<Integer> getNoImages() {
		return botManager.getNoImages();
	}
	public LiveData<Integer> getBotState() {
		return botManager.getBotState();
	}

	/**
	 * Connect to the given peripheral.
	 *
	 * @param target the target device.
	 */
	public void connect(@NonNull final DiscoveredBluetoothDevice target) {
		// Prevent from calling again when called again (screen orientation changed).
		if (device == null) {
			device = target.getDevice();
			final LogSession logSession = Logger
					.newSession(getApplication(), null, target.getAddress(), target.getName());
			botManager.setLogger(logSession);
			reconnect();
		}
	}

	/**
	 * Reconnects to previously connected device.
	 * If this device was not supported, its services were cleared on disconnection, so
	 * reconnection may help.
	 */
	public void reconnect() {
		if (device != null) {
			botManager.connect(device)
					.retry(3, 100)
					.useAutoConnect(false)
					.enqueue();
		}
	}

	/**
	 * Disconnect from peripheral.
	 */
	private void disconnect() {
		device = null;
		botManager.disconnect().enqueue();
	}


	public void setNoImages(final int no) {
		botManager.sendNoOfImages(no);
	}
	public void setClick() {
		botManager.setClickImage();
	}
	public void setBotState(final int state) {
		botManager.setBotState(state);
	}


	@Override
	protected void onCleared() {
		super.onCleared();
		if (botManager.isConnected()) {
			disconnect();
		}
	}
}
