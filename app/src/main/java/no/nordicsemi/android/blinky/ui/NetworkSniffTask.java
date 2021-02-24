package no.nordicsemi.android.blinky.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.Arrays;

class NetworkSniffTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "nstask";

    private final WeakReference<Context> mContextRef;

    public NetworkSniffTask(Context context) {
        mContextRef = new WeakReference<Context>(context);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Log.d(TAG, "Let's sniff the network");

        try {
            Context context = mContextRef.get();

            if (context != null) {

                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                DhcpInfo connectionInfo = wm.getDhcpInfo();


                InetAddress host = InetAddress.getByName(intToIp(connectionInfo.dns1));
                byte[] ip = host.getAddress();

                for (int i = 0; i < 255; i++) {
                    ip[3] = (byte) i;

                    InetAddress address = InetAddress.getByAddress(ip);

                    if (address.isReachable(100)) {
                        System.out.println(address + " machine is turned on and can be pinged" + address.getCanonicalHostName() + " " + address.getHostName() + " " + Arrays.toString(address.getAddress()) + " " + address.getHostAddress());
                    } else if (!address.getHostAddress().equals(address.getHostName())) {
                        System.out.println(address + " machine is known in a DNS lookup");
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Well that's not good.", t);
        }

        return null;
    }

    public String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 24) & 0xFF);
    }
}