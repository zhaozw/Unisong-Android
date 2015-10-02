package io.unisong.android.network;

import android.os.Handler;
import android.util.Log;

import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * This class handles the discover of local Unisong devices. It will send broadcast requests over the LAN
 * and listen to the available wifi networks/see if any of those are Unisong local hotspots
 * Created by Ethan on 10/1/2015.
 */
public class DiscoveryHandler {

    private static DiscoveryHandler sInstance;

    public static DiscoveryHandler getInstance(){
        return sInstance;
    }

    public static void setInstance(DiscoveryHandler handler){
        sInstance = handler;
    }

    private ConnectionUtils mConnection;
    private final static String LOG_TAG = DiscoveryHandler.class.getSimpleName();
    private DatagramSocket mSocket;
    private Handler mHandler;

    public DiscoveryHandler(){
        try {
            mSocket = new DatagramSocket(CONSTANTS.DISCOVERY_PORT);
            mSocket.setBroadcast(true);
        } catch (SocketException e){
            e.printStackTrace();
            Log.d(LOG_TAG, "Creating discovery socket failed!");
        }

        mConnection = ConnectionUtils.getInstance();
    }

    public Thread getDiscoveryThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                sendDiscoveryPacket()
            }
        });
    }



}
