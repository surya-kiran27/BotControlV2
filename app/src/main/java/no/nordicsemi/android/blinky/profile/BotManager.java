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

package no.nordicsemi.android.blinky.profile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.UUID;

import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.livedata.ObservableBleManager;
import no.nordicsemi.android.blinky.profile.callback.BlinkyImagesDataCallback;
import no.nordicsemi.android.blinky.profile.callback.BotState;
import no.nordicsemi.android.blinky.profile.callback.ClickImages;
import no.nordicsemi.android.blinky.profile.callback.ClickImagesCallBack;
import no.nordicsemi.android.blinky.profile.data.BotWrite;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

public class BotManager extends ObservableBleManager {
	/** Nordic Blinky Service UUID. */
	/** BUTTON characteristic UUID. */
	public final static UUID	SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
	private final static UUID	CHARACTERISTIC_UUID1 = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
	private final static UUID	CHARACTERISTIC_UUID2 = UUID.fromString("fac0db31-730a-4c90-9424-3f802a47c021");
	private final static UUID	CHARACTERISTIC_UUID3 = UUID.fromString("4892d430-5a19-43ef-a5ce-d62ab8d43f50");
	private final MutableLiveData<Integer> images = new MutableLiveData<>();
	private final MutableLiveData<Integer> clickState = new MutableLiveData<>();
	private final MutableLiveData<Integer>  botState = new MutableLiveData<>();

	private BluetoothGattCharacteristic imagesCharacteristic, clickCharacteristic,stateCharacterstic;
	private LogSession logSession;
	private boolean supported;

	public BotManager(@NonNull final Context context) {
		super(context);
	}

	public final LiveData<Integer> getNoImages() {
		return images;
	}

	public final LiveData<Integer> getClickState() {
		return clickState;
	}
	public final LiveData<Integer> getBotState() {
		return botState;
	}


	@NonNull
	@Override
	protected BleManagerGattCallback getGattCallback() {
		return new BlinkyBleManagerGattCallback();
	}

	/**
	 * Sets the log session to be used for low level logging.
	 * @param session the session, or null, if nRF Logger is not installed.
	 */
	public void setLogger(@Nullable final LogSession session) {
		logSession = session;
	}

	@Override
	public void log(final int priority, @NonNull final String message) {
		// The priority is a Log.X constant, while the Logger accepts it's log levels.
		Logger.log(logSession, LogContract.Log.Level.fromPriority(priority), message);
	}



	@Override
	protected boolean shouldClearCacheWhenDisconnected() {
		return !supported;
	}

	private	final ClickImages clickCallback = new ClickImages() {

		@Override
		public void onClickChanged(@NonNull BluetoothDevice device, int click) {
			clickState.setValue(click);
		}


	};
	private	final BotState botStateCallback = new BotState() {
		@Override
		public void onBotStateChanged(@NonNull BluetoothDevice device, int state) {
			botState.setValue(state);
		}
	};


	private final BlinkyImagesDataCallback imagesCallback = new BlinkyImagesDataCallback() {
		@Override
		public void onImagesChanged(@NonNull BluetoothDevice device, int no) {
			images.setValue(no);
		}

		@Override
		public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
										  @NonNull final Data data) {
			// Data can only invalid if we read them. We assume the app always sends correct data.
			log(Log.WARN, "Invalid data received: " + data);
		}
	};

	/**
	 * BluetoothGatt callbacks object.
	 */
	private class BlinkyBleManagerGattCallback extends BleManagerGattCallback {
		@Override
		protected void initialize() {
			setNotificationCallback(clickCharacteristic).with(clickCallback);
			setNotificationCallback(stateCharacterstic).with(botStateCallback);
			readCharacteristic(imagesCharacteristic).with(imagesCallback).enqueue();
			readCharacteristic(clickCharacteristic).with(clickCallback).enqueue();
			readCharacteristic(stateCharacterstic).with(botStateCallback).enqueue();
			enableNotifications(clickCharacteristic).enqueue();
			enableNotifications(stateCharacterstic).enqueue();

		}

		@Override
		public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {

			final BluetoothGattService service = gatt.getService(SERVICE_UUID);
			Log.i("test", "isRequiredServiceSupported: "+service);
			if (service != null) {
				imagesCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID1);
				clickCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID2);
				stateCharacterstic=service.getCharacteristic(CHARACTERISTIC_UUID3);
			}

			boolean writeRequest = false;
			boolean writeRequest2 = false;

			if (imagesCharacteristic != null) {
				final int rxProperties = imagesCharacteristic.getProperties();
				writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
			}
			if (stateCharacterstic != null) {
				final int rxProperties = stateCharacterstic.getProperties();
				writeRequest2 = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
			}


			supported = imagesCharacteristic != null && clickCharacteristic != null &&stateCharacterstic!=null && writeRequest&&writeRequest2;
			return supported;
		}

		@Override
		protected void onDeviceDisconnected() {
			imagesCharacteristic = null;
			clickCharacteristic = null;
		}
	}


	public void sendNoOfImages(final int no) {
		// Are we ?
		if (imagesCharacteristic == null)
			return;
		writeCharacteristic(imagesCharacteristic,
				 BotWrite.sendImages(no))
				.with(imagesCallback).enqueue();
	}

	public void setClickImage(){
		if (clickCharacteristic==null)
			return;
		writeCharacteristic(clickCharacteristic,BotWrite.setClick(0)).with(clickCallback).enqueue();
	}
	public void setBotState(int state){
		if (stateCharacterstic==null)
			return;
		writeCharacteristic(stateCharacterstic,BotWrite.setState(state)).with(botStateCallback).enqueue();
	}
}
