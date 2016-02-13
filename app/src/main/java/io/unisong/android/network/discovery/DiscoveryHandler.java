package io.unisong.android.network.discovery;

import android.os.Handler;
import android.util.Log;

import java.net.DatagramSocket;
import java.net.SocketException;

import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.ConnectionUtils;

/**
 * This class handles the discover of local Unisong devices. It will send broadcast requests over the LAN
 * and listen to the available wifi networks/see if any of those are Unisong local hotspots
 * Created by Ethan on 10/1/2015.
 */
public class DiscoveryHandler {

    private static DiscoveryHandler instance;

    public static DiscoveryHandler getInstance(){
        return instance;
    }

    public static void setInstance(DiscoveryHandler handler){
        instance = handler;
    }

    private ConnectionUtils connection;
    private final static String LOG_TAG = DiscoveryHandler.class.getSimpleName();
    private DatagramSocket socket;
    private Handler mHandler;
    private boolean stop = false;

    public DiscoveryHandler(){
        try {
            socket = new DatagramSocket(CONSTANTS.DISCOVERY_PORT);
            socket.setBroadcast(true);
        } catch (SocketException e){
            e.printStackTrace();
            Log.d(LOG_TAG, "Creating discovery socket failed!");
        }

        connection = ConnectionUtils.getInstance();
    }

    public Thread getDiscoveryThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                while(!stop) {
                    sendDiscoveryPacket();
                    listenForDiscoveryPackets();
                    // TODO : implement this stuff
                    stop = true;
                }
            }
        });
    }

    private void sendDiscoveryPacket(){

    }

    public void listenForDiscoveryPackets(){

    }

    public void destroy(){
        stop = true;

    }


}
