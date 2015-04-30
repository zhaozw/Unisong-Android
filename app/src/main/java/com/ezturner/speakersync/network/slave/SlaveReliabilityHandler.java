package com.ezturner.speakersync.network.slave;


import android.os.Handler;
import android.util.Log;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    private ArrayList<Integer> mPacketsReRequested;

    //The socket for retransmitting packets that need to be sent over the network
    private DatagramSocket mDatagramSocket;

    //The parent AudioListener object, used to get packets to be retransmitted
    private AudioListener mListener;

    //The list of packets that need to be retransmit
    private List<Integer> mPacketsToRetransmit;


    public SlaveReliabilityHandler(InetAddress address, int broadcastPort , AudioListener listener){
        mMasterAddress = address;

        mListener = listener;
        try{
            mDatagramSocket = new DatagramSocket();
            mDatagramSocket.setBroadcast(true);
        } catch (SocketException e){
            e.printStackTrace();
        }

        Log.d(LOG_TAG , mDatagramSocket.toString());

        mTopPacket = 0;
        mPacketsReceived = new ArrayList<>();
        mPacketsToRetransmit = new ArrayList<>();
        mPacketsRecentlyRequested = new HashMap<>();
        mPacketsNotReceived = new HashMap<>();
        mPacketsToRequest = new HashMap<>();

        mPacketsReRequested = new ArrayList<>();
        mHandler = new Handler();

        mHandler.postDelayed(mPacketRequester , 30);

        mRequesting = true;

        mThread = getThread();
        mThread.start();
    }

    Runnable mPacketRequester = new Runnable(){
        @Override
        public void run() {

            ArrayList<Integer> packetsSent = new ArrayList<>();
            synchronized (mPacketsToRequest){
                for (Map.Entry<Integer, Long> entry : mPacketsToRequest.entrySet()) {
                    if (System.currentTimeMillis() - entry.getValue() >= 10) {
                        requestPacket(entry.getKey());
                        packetsSent.add(entry.getKey());
                    }
                }
            }


            ArrayList<Integer> packetsToRemove = new ArrayList<>();
            synchronized (mPacketsRecentlyRequested){
                for (Map.Entry<Integer, Long> entry : mPacketsRecentlyRequested.entrySet()){
                    long timeSince = System.currentTimeMillis() - entry.getValue();
                    if (timeSince >= 250 && !mPacketsReRequested.contains(entry.getKey()) &&  timeSince <= 400) {
                        mPacketsReRequested.add(entry.getKey());
                        requestPacket(entry.getKey());
                    } else if(timeSince > 400){
                        packetsSent.add(entry.getKey());
                    }
                }
            }

            for(Integer i: packetsToRemove){
                mPacketsRecentlyRequested.remove(i);
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
        String append ="";
        if(mPacketsReRequested.contains(packetID)){
            append += " once more";
        }

        Log.d(LOG_TAG, "Requesting Packet #" + packetID  + append);
        synchronized (mOutStream){

            try {
                byte[] data = NetworkUtilities.combineArrays(new byte[]{CONSTANTS.TCP_REQUEST_ID} , ByteBuffer.allocate(4).putInt(packetID).array());
                mOutStream.write(data , 0 , 5);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(!mPacketsRecentlyRequested.containsKey(packetID)) {
            mPacketsRecentlyRequested.put(packetID, System.currentTimeMillis());
        }
    }

    //The function to close the socket
    public void closeSocket(){
        try {
            mSocket.close();
            mSocket = null;
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //A function to connect to a Master phone,
    private void connectToHost(InetAddress address){
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

        listenForCommands();
    }

    private void listenForCommands(){

        byte[] data = new byte[5];
        try {
            while (mInStream.read(data) != 0){
                switch (data[0]){
                    case CONSTANTS.TCP_COMMAND_RETRANSMIT:
                        int ID = ByteBuffer.wrap(Arrays.copyOfRange(data , 1, 5)).getInt();
                        retransmitPacket(ID);
                        break;

                }

            }

        } catch (IOException e){
            //TODO: figure out when this is called and how to deal with it
            e.printStackTrace();
        }
    }

    private void retransmitPacket(int packetID){
        DatagramPacket packet = mListener.getPacket(packetID);
        synchronized (mDatagramSocket){
            try {
                mDatagramSocket.send(packet);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void packetReceived(int packetID){

        mPacketsReceived.add(packetID);

        acknowledgePacket(packetID);

        synchronized (mPacketsRecentlyRequested) {
            if (mPacketsRecentlyRequested.containsKey(packetID)) {

                long diff = System.currentTimeMillis() - mPacketsRecentlyRequested.get(packetID);
                Log.d(LOG_TAG, "Packet #" + packetID + " received after " + diff + "ms" + "");


                synchronized (mPacketsNotReceived) {
                    mPacketsNotReceived.remove(packetID);
                }
                mPacketsRecentlyRequested.remove(packetID);
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

    private void acknowledgePacket(int packetID){
        synchronized (mOutStream){
            try {
                mOutStream.write(NetworkUtilities.combineArrays(new byte[]{CONSTANTS.TCP_ACK_ID} , ByteBuffer.allocate(4).putInt(packetID).array()) , 0 , 5);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }


}
