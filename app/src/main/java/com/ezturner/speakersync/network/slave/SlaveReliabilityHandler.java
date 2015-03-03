package com.ezturner.speakersync.network.slave;


import com.ezturner.speakersync.network.CONSTANTS;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;

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



    public SlaveReliabilityHandler(InetAddress address){
        mMasterAddress = address;
        connectToHost(mMasterAddress);
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


}
