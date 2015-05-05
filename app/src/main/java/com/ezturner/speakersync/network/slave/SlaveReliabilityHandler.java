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

    //Whether we can request packets. Is false until
    private boolean mCanRequest;


    public SlaveReliabilityHandler(InetAddress address, int broadcastPort , AudioListener listener){
        mMasterAddress = address;

        mListener = listener;
        try{
            mDatagramSocket = new DatagramSocket();
            mDatagramSocket.setBroadcast(true);
        } catch (SocketException e){
            e.printStackTrace();
        }


        mTopPacket = 0;
        mPacketsReceived = new ArrayList<>();
        mPacketsToRetransmit = new ArrayList<>();
        mPacketsRecentlyRequested = new HashMap<>();
        mPacketsNotReceived = new HashMap<>();
        mPacketsToRequest = new HashMap<>();

        mPacketsReRequested = new ArrayList<>();
        mHandler = new Handler();

        mCanRequest = false;

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
                    if(timeSince >= 250){
                        requestPacket(entry.getKey());

                        if(!mPacketsReRequested.contains(entry.getKey())){
                            mPacketsReRequested.add(entry.getKey());
                        }
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
                mOutStream.write(CONSTANTS.TCP_REQUEST_ID);
                byte[] data = ByteBuffer.allocate(4).putInt(packetID).array();
                mOutStream.write(data , 0 , 4);
            } catch (IOException e){
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
        byte type = -1;
        try {

            type = mInStream.readByte();
        } catch (IOException e){
            e.printStackTrace();
        }

        try {
            while (type != -1){
                Log.d(LOG_TAG , "Listening for TCP Data, type is: " + type);
                switch (type){
                    case CONSTANTS.TCP_COMMAND_RETRANSMIT:
                        listenRetransmit();
                        break;
                    case CONSTANTS.TCP_SONG_START:
                        listenSongStart();
                        break;
                    case CONSTANTS.TCP_SONG_IN_PROGRESS:
                        listenSongInProgress();
                        break;

                }

            }

        } catch (IOException e){
            //TODO: figure out when this is called and how to deal with it
            e.printStackTrace();
        }
    }

    //Listens for the retransmit data/packet ID
    private void listenRetransmit() throws IOException {
        Log.d(LOG_TAG , "Listening for Retransmit");
        byte[] data = new byte[4];
        if(mInStream.read(data) != -1) {
            int ID = ByteBuffer.wrap(data).getInt();
            retransmitPacket(ID);
        }
    }

    //Listens for the song start data.
    private void listenSongStart() throws IOException{
        Log.d(LOG_TAG , "Song Start identifier started");
        byte[] data = new byte[13];

        mCanRequest = true;
        try{
            mInStream.read(data);
        } catch (IOException e){
            e.printStackTrace();
        }
        byte[] playTimeArr = Arrays.copyOfRange(data, 0, 8);

        long startTime = ByteBuffer.wrap(playTimeArr).getLong();

        byte[] channelsArr = Arrays.copyOfRange(data, 8, 12);

        int channels = ByteBuffer.wrap(channelsArr).getInt();

        mListener.startSong(startTime , channels, data[12] , 0);
        Log.d(LOG_TAG , "Song Starting!");
    }

    private void listenSongInProgress() throws IOException{
        Log.d(LOG_TAG , "Song in progress");
        byte[] data = new byte[17];

        mCanRequest = true;
        try{
            mInStream.read(data);
        } catch (IOException e){
            e.printStackTrace();
        }

        byte[] playTimeArr = Arrays.copyOfRange(data, 0, 8);

        long startTime = ByteBuffer.wrap(playTimeArr).getLong();

        byte[] channelsArr = Arrays.copyOfRange(data, 8, 12);

        int channels = ByteBuffer.wrap(channelsArr).getInt();

        byte[] currentPacketArr = Arrays.copyOfRange(data, 12, 16);

        int currentPacket = ByteBuffer.wrap(channelsArr).getInt();

        mListener.startSong(startTime , channels, data[16] , currentPacket);

        mTopPacket = currentPacket;

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
            if (packetID > mTopPacket && mCanRequest) {
                for (int i = mTopPacket; i < packetID; i++){
                    if (!mPacketsReceived.contains(i)){
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
