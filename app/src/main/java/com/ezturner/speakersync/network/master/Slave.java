package com.ezturner.speakersync.network.master;

import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ezturner on 4/29/2015.
 */
public class Slave {

    private static final String LOG_TAG = Slave.class.getSimpleName();
    private InetAddress mAddress;

    //A list of all of the packets that this slave has received and has in memory.
    private List<Integer> mPacketsReceived;
    private Map<Integer, Long> mPacketsRebroadcasted;

    public Slave(String ip){
        try {
            mAddress = Inet4Address.getByName(ip.split(":")[0].substring(1));
        } catch (UnknownHostException e){
            e.printStackTrace();
            Log.e(LOG_TAG, "Unknown host address when creating slave : " + ip);
        }

        mPacketsRebroadcasted = new HashMap<>();
        mPacketsReceived = new ArrayList<>();
    }

    public void packetReceived(int ID){
        mPacketsReceived.add(ID);
        if(mPacketsRebroadcasted.containsKey(ID)){
            mPacketsRebroadcasted.remove(ID);
        }
    }

    public boolean hasPacket(int ID){
        return mPacketsReceived.contains(ID);
    }

    public void packetHasBeenRebroadcasted(int ID){
        if(!mPacketsReceived.contains(ID)) {
            mPacketsRebroadcasted.put(ID, System.currentTimeMillis());
        }
    }

    public String toString(){
        return "Slave, IP: " + mAddress.toString();
    }

    public List<Integer> getPacketsToBeReSent(){
        List<Integer> ids = new ArrayList<>();

        ArrayList<Integer> packetsSent = new ArrayList<>();
        synchronized (mPacketsRebroadcasted){
            for (Map.Entry<Integer, Long> entry : mPacketsRebroadcasted.entrySet()) {
                if (System.currentTimeMillis() - entry.getValue() >= 150) {
                    ids.add(entry.getKey());
                    packetsSent.add(entry.getKey());
                }
            }
        }

        for(Integer i : packetsSent){
            mPacketsRebroadcasted.remove(i);
            mPacketsRebroadcasted.put(i, System.currentTimeMillis());
        }

        return ids;
    }

}
