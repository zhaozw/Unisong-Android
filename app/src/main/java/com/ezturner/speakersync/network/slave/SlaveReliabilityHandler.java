package com.ezturner.speakersync.network.slave;


import android.os.Handler;
import android.util.Log;

import com.ezturner.speakersync.network.CONSTANTS;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ezturner on 2/16/2015.
 */
public class SlaveReliabilityHandler {

    private String LOG_TAG = "SlaveReliabilityHandler";

    //The address of the Master that we will connect to.
    private InetAddress mMasterAddress;

    //The socket that will connect to the master
    private Socket mSocket;

    private DataOutputStream mOutStream;

    private DataInputStream mInStream;

    private Thread mThread;


    private Map<Integer , Long> mPacketsRecentlyRequested;
    private ArrayList<Integer> mPacketsReceived;

    //The handler that checks if a packet that was asked for has been received
    private Handler mHandler;

    public SlaveReliabilityHandler(InetAddress address){
        mMasterAddress = address;

        mPacketsReceived = new ArrayList<Integer>();
        mPacketsRecentlyRequested = new HashMap<Integer, Long>();

        mHandler = new Handler();
    }

    private Thread getThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                connectToHost(mMasterAddress);
            }
        });
    }

    //Sends a request to resend a packet
    public void requestPacket(int packetID){
        Log.d(LOG_TAG, "Requesting Packet #" + packetID);
        synchronized (mOutStream) {
            try {
                mOutStream.writeInt(packetID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void closeSocket(){
        try {
            mSocket.close();
            mSocket = null;
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void connectToHost(InetAddress address){
        try{
            if(mSocket != null){
                mSocket.close();
            }

            mSocket = new Socket(address , CONSTANTS.RELIABILITY_PORT);
            mOutStream = new DataOutputStream(mSocket.getOutputStream());
            mInStream = new DataInputStream(mSocket.getInputStream());
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void packetReceived(int packetID){
        Log.d(LOG_TAG , "Packet #" + packetID);
        mPacketsReceived.add(packetID);
        ArrayList<Integer> packetsToRequest = new ArrayList<>();

        int i = 5;
        packetID-= 5;
        while(i >= 0){
            i--;
            if(!mPacketsReceived.contains(packetID) && !mPacketsRecentlyRequested.containsKey(packetID) && packetID >=0){
                packetsToRequest.add(packetID);
                mPacketsRecentlyRequested.put(packetID, System.currentTimeMillis());
                packetID--;
                i=5;
            }
        }

        for(Integer packetid : packetsToRequest){
            requestPacket(packetid);
        }

    }

    private Thread getRequestThread(final int packetID){
        return new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                if(packetID != 0 && !mPacketsReceived.contains(packetID -1)){
                    requestPacket(packetID);
                    boolean requesting = true;
                    int id = packetID - 2;
                    while(id > 0 && requesting){
                        if(mPacketsReceived.contains(id)){
                            requestPacket(id);
                        } else {
                            requesting = false;
                        }
                        id--;
                    }
                }
            }
        });
    }


}
