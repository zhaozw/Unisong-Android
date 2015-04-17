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
import java.nio.ByteBuffer;
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

    private Map<Integer , Long> mPacketsRecentlyRequested;

    private int mTopPacket;
    private ArrayList<Integer> mPacketsReceived;
    private Map<Integer , Long> mPacketsNotReceived;

    private Map<Integer, Long> mPacketsToRequest;

    //The handler that checks if a packet that was asked for has been received
    private Handler mHandler;

    private boolean mRequesting;
    private Thread mThread;

    public SlaveReliabilityHandler(InetAddress address){
        mMasterAddress = address;

        mTopPacket = 0;
        mPacketsReceived = new ArrayList<Integer>();
        mPacketsRecentlyRequested = new HashMap<>();
        mPacketsNotReceived = new HashMap<>();
        mPacketsToRequest = new HashMap<>();

        mHandler = new Handler();

        mHandler.postDelayed(mPacketRequester , 30);

        mRequesting = true;

        mThread = getThread();
        mThread.start();
    }

    Runnable mPacketRequester = new Runnable() {
        @Override
        public void run() {

            ArrayList<Integer> packetsSent = new ArrayList<Integer>();
            synchronized (mPacketsToRequest) {
                for (Map.Entry<Integer, Long> entry : mPacketsToRequest.entrySet()) {
                    if (System.currentTimeMillis() - entry.getValue() >= 10) {
                        requestPacket(entry.getKey());
                        packetsSent.add(entry.getKey());
                    }
                }
            }

            for(Integer i : packetsSent){
                mPacketsToRequest.remove(i);
            }
            if(mRequesting){
                mHandler.postDelayed(mPacketRequester , 10);
            }
        }
    };

    private void stop(){
        mRequesting = false;
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
                mOutStream.write(ByteBuffer.allocate(4).putInt(packetID).array(), 0, 4);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mPacketsRecentlyRequested.put(packetID , System.currentTimeMillis());
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

        mPacketsReceived.add(packetID);

        synchronized (mPacketsRecentlyRequested) {
            if (mPacketsRecentlyRequested.containsKey(packetID)) {

                long diff = System.currentTimeMillis() - mPacketsRecentlyRequested.get(packetID);
                Log.d(LOG_TAG, "Packet #" + packetID + " received after " + diff + "ms" + "");


                synchronized (mPacketsNotReceived) {
                    mPacketsNotReceived.remove(packetID);
                }
            }
        }

        synchronized (mPacketsToRequest) {
            if (packetID > mTopPacket) {
                for (int i = mTopPacket; i < packetID; i++){
                    if (!mPacketsReceived.contains(i)) {
                        mPacketsToRequest.put(i, System.currentTimeMillis());
                    }
                }

                mTopPacket = packetID;
            }

            if (mPacketsToRequest.containsKey(packetID)){
                mPacketsToRequest.remove(packetID);
            }
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
