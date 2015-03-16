package com.ezturner.speakersync.network.slave;


import com.ezturner.speakersync.network.CONSTANTS;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by ezturner on 2/16/2015.
 */
public class SlaveReliabilityHandler {

    //The address of the Master that we will connect to.
    private InetAddress mMasterAddress;

    //The socket that will connect to the master
    private Socket mSocket;

    private DataOutputStream mOutStream;

    private DataInputStream mInStream;

    private Thread mThread;


    private ArrayList<Integer> mPacketsReceived;

    public SlaveReliabilityHandler(InetAddress address){
        mMasterAddress = address;
        mPacketsReceived = new ArrayList<Integer>();
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
    public void requestPacket(int packetId){
        try {
            mOutStream.writeInt(packetId);
        } catch(IOException e){
            e.printStackTrace();
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

    public  void  packetReceived(int packetID){
        mPacketsReceived.add(packetID);

        //TODO : implement a reliability listener

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
