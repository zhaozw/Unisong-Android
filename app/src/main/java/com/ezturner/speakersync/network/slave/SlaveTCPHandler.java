package com.ezturner.speakersync.network.slave;


import android.os.Handler;
import android.util.Log;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.packets.tcp.TCPAcknowledgePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPFramePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPRequestPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPResumePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPRetransmitPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPSongInProgressPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPSongStartPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPSwitchMasterPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ezturner on 2/16/2015.
 */
public class SlaveTCPHandler {

    private String LOG_TAG = SlaveTCPHandler.class.getSimpleName();

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

    private boolean mRunning;
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


    public SlaveTCPHandler(InetAddress address, int broadcastPort, AudioListener listener){
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

        mHandler = new Handler();

        mCanRequest = false;

        mHandler.postDelayed(mPacketRequester , 30);

        mRunning = true;

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

            for(Integer i : packetsSent){
                mPacketsToRequest.remove(i);
            }
            if(mRunning){
                mHandler.postDelayed(mPacketRequester , 10);
            }
        }
    };

    private void stop(){
        mRunning = false;
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

        TCPRequestPacket.send(mOutStream, packetID);

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
            Log.d(LOG_TAG , e.toString());
            e.printStackTrace();
        }


        while (type != -1 && mRunning){
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
                case CONSTANTS.TCP_FRAME:
                    listenFrame();
                    break;
                case CONSTANTS.TCP_PAUSE:
                    listenPause();
                    break;
                case CONSTANTS.TCP_RESUME:
                    listenResume();
                    break;
                case CONSTANTS.TCP_END_SESSION:
                    endSession();
                    break;
                //This Slave is now the session master.
                case CONSTANTS.TCP_ASSIGN_MASTER:
                    assignMaster();
                    break;
                case CONSTANTS.TCP_SEEK:
                    listenSeek();
                    break;
                case CONSTANTS.TCP_SWITCH_MASTER:
                    switchMaster();
                    break;
            }

            try {
                type = mInStream.readByte();
            } catch (IOException e){
                Log.d(LOG_TAG , e.toString());
                e.printStackTrace();
            }
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
            if (packetID > mTopPacket && mCanRequest && packetID < mTopPacket + 100) {
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
        TCPAcknowledgePacket.send(mOutStream, packetID);
    }

    //Listens for the Seek command for the song
    private void listenSeek(){

    }
    //Listens for the retransmit data/packet ID
    private void listenRetransmit(){
        Log.d(LOG_TAG, "Listening for Retransmit");

        TCPRetransmitPacket packet = new TCPRetransmitPacket(mInStream);
        retransmitPacket(packet.getPacketToRetransmit());
    }

    //Listens for the song start data.
    private void listenSongStart(){
        Log.d(LOG_TAG, "Song Start identifier received");

        TCPSongStartPacket packet = new TCPSongStartPacket(mInStream);

        mCanRequest = true;

        mListener.startSong(packet.getSongStartTime(), packet.getChannels(), packet.getStreamID(), 0);
        Log.d(LOG_TAG, "Song Starting!");
    }

    private void listenSongInProgress(){
        Log.d(LOG_TAG , "Song in progress");

        TCPSongInProgressPacket packet = new TCPSongInProgressPacket(mInStream);
        mCanRequest = true;


        mListener.startSong(packet.getSongStartTime() , packet.getChannels(), packet.getStreamID() , packet.getCurrentPacket());

        mTopPacket = packet.getCurrentPacket();

    }

    private void listenFrame(){
        TCPFramePacket packet = new TCPFramePacket(mInStream);

        mListener.addFrame(packet.getFrame());

        packetReceived(packet.getFrame().getID());
        //TODO: handle frame here
    }

    private void listenPause(){
        Log.d(LOG_TAG, "Pause Received");
        mListener.pause();
        //TODO: handle pause
    }

    private void listenResume(){
        TCPResumePacket packet = new TCPResumePacket(mInStream);

        Log.d(LOG_TAG , "Resume time is : " + packet.getResumeTime() + " and song start time is : " + packet.getNewSongStartTime());
        mListener.resume(packet.getResumeTime(), packet.getNewSongStartTime());
    }

    private void switchMaster(){
        TCPSwitchMasterPacket switchMasterPacket = new TCPSwitchMasterPacket(mInStream);

    }

    //Sets this as the master
    private void assignMaster(){

    }

    private void endSession(){

    }
    
    public synchronized void destroy(){
        mRunning = false;
        mThread = null;

    }

}
