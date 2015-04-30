package com.ezturner.speakersync.network.master;

import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ezturner on 4/29/2015.
 */
public class Slave {

    private static final String LOG_TAG = Slave.class.getSimpleName();
    private InetAddress mAddress;

    //A list of all of the packets that this slave has received and has in memory.
    private List<Integer> mPacketsReceived;

    public Slave(String ip){
        try {
            mAddress = Inet4Address.getByName(ip);
        } catch (UnknownHostException e){
            e.printStackTrace();
            Log.e(LOG_TAG, "Unknown host address when creating slave : " + ip);
        }

        mPacketsReceived = new ArrayList<>();
    }

    public void packetReceived(int ID){
        mPacketsReceived.add(ID);
    }

    public boolean hasPacket(int ID){
        return mPacketsReceived.contains(ID);
    }



}
