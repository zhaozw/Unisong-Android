package com.ezturner.speakersync.network;

import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioStatePublisher;
import com.ezturner.speakersync.audio.master.CurrentSongInfo;
import com.ezturner.speakersync.network.master.MasterTCPHandler;
import com.ezturner.speakersync.network.master.transmitter.LANTransmitter;
import com.ezturner.speakersync.network.packets.tcp.TCPAcknowledgePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPEndSongPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPFramePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPPausePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPRequestPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPResumePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPSeekPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPSongInProgressPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPSongStartPacket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ezturner on 4/29/2015.
 */
public class Client {

    private static final String LOG_TAG = Client.class.getSimpleName();
    private InetAddress mAddress;

    //A list of all of the packets that this slave has received and has in memory.
    private List<Integer> mPacketsReceived;
    private Map<Integer, Long> mPacketsRebroadcasted;
    private boolean mConnected;
    private Socket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    private boolean mThreadRunning;
    private Thread mListenThread;
    private MasterTCPHandler mMasterTCPHandler;

    //The Singletons
    private AudioStatePublisher mAudioStatePublisher;
    private TimeManager mTimeManager;
    private CurrentSongInfo mCurrentSongInfo;

    private LANTransmitter mTransmitter;


    /**
     * This is a constructor for a locally connected client, from the master to client
     * this constructor will be called by the master
     * @param ip The device IP
     * @param socket the socket that it is connected to
     * @param parent the TCP handler that holds us as its parent
     * @param transmitter the LANTransmitter that we are using for this session
     */
    //TODO : Add mode for server connection
    public Client(String ip, Socket socket, MasterTCPHandler parent, LANTransmitter transmitter){

        mTransmitter = transmitter;

        //Get the singleton objects.
        mTimeManager = TimeManager.getInstance();
        mCurrentSongInfo = CurrentSongInfo.getInstance();
        mAudioStatePublisher = AudioStatePublisher.getInstance();

        mMasterTCPHandler = parent;
        mSocket = socket;

        try{
            mInputStream = new BufferedInputStream(mSocket.getInputStream());
            mOutputStream = new BufferedOutputStream(mSocket.getOutputStream());
        } catch (IOException e){
            e.printStackTrace();
            mConnected = false;
        }

        mConnected = true;

        try {
            mAddress = Inet4Address.getByName(ip.split(":")[0].substring(1));
        } catch (UnknownHostException e){
            e.printStackTrace();
            Log.e(LOG_TAG, "Unknown host address when creating slave : " + ip);
        }

        mPacketsRebroadcasted = new HashMap<>();
        mPacketsReceived = new ArrayList<>();

        mListenThread = getListenThread();
        mListenThread.start();
    }


    /**
     * The constructor for client information for a client
     *
     */
    public Client(String ip, String name, String phonenumber){

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
        return "Client, IP: " + mAddress.toString();
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

    //Start listening for packets
    private Thread getListenThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                mThreadRunning = true;
                listenForPackets();
                mThreadRunning = false;
            }
        });
    }

    private void listenForPackets(){
        //Handle new data coming in from a Reliability socket
        //TODO: see if we need to get rid of the port for this to work

        //If we are playing, then just tell the client that we are playing a song in progress
        if(mAudioStatePublisher.getState() == AudioStatePublisher.PLAYING) {

            sendSongInProgress();

        } else if(mAudioStatePublisher.getState() == AudioStatePublisher.PAUSED){

            //If we are paused, tell the client we are playing a song in progress and then pause
            sendSongInProgress();
            pause();

        } else if(mAudioStatePublisher.getState() == AudioStatePublisher.RESUME){

            //If we are in the resume stage, then wait 10ms and notify of a song in progress
            try {
                synchronized (this) {
                    this.wait(10);
                }
            } catch (InterruptedException e){

            }
            sendSongInProgress();
        }

        byte type;
        synchronized (mInputStream) {
            try {
                type = (byte) mInputStream.read();
            } catch (IOException e){
                e.printStackTrace();
                mConnected = false;
                return;
            }
        }

        while(type != -1 && mConnected){
            handleDataReceived(type);
            try {
                type = (byte) mInputStream.read();
            } catch (IOException e){
                e.printStackTrace();
                mConnected = false;
                return;
            }
        }

    }

    //Hanldes the identifying byte and redirects it to the right method
    private void handleDataReceived(byte type){
        switch (type){
            case CONSTANTS.TCP_REQUEST:
                int packetID = new TCPRequestPacket(mInputStream).getPacketRequested();
                if(packetID != -1)  mMasterTCPHandler.rebroadcastFrame(packetID);
                break;
            case CONSTANTS.TCP_ACK:
                int ID = new TCPAcknowledgePacket(mInputStream).getPacketAcknowledged();
                if(ID != -1)    packetReceived(ID);
                break;
        }
    }

    //Tells this slave that a song is currently in progress
    public void sendSongInProgress() {
        Log.d(LOG_TAG, "Sending Song Start to "  + toString());
        DataOutputStream outputStream;
        synchronized (mOutputStream){
            //Send out the Song In Progress TCP packet.
            TCPSongInProgressPacket.send(mOutputStream, mTimeManager.getSongStartTime(), mCurrentSongInfo.getChannels(),
                    mTransmitter.getNextPacketSendID(), (byte) 0);
        }
    }

    //Notifies this slave that a song is starting
    public void notifyOfSongStart(){
        synchronized (mOutputStream){
            TCPSongStartPacket.send(mOutputStream , mTimeManager.getSongStartTime() , mCurrentSongInfo.getChannels() , (byte) 0 );
        }
    }

    public void retransmitPacket(int packetID){
        synchronized (mOutputStream){
            TCPRequestPacket.send(mOutputStream, packetID);
        }
    }

    public void sendFrame(AudioFrame frame){
        synchronized (mOutputStream){
            TCPFramePacket.send(mOutputStream , frame, (byte) 0);
        }
    }

    public boolean isConnected(){
        return mConnected;
    }

    public void pause(){
        synchronized (mOutputStream){
            TCPPausePacket.send(mOutputStream);
        }
    }

    public void resume(long resumeTime, long newSongStartTime){
        synchronized (mOutputStream){
            TCPResumePacket.send(mOutputStream , resumeTime , newSongStartTime );
        }
    }

    public void seek(long seekTime){
        synchronized (mOutputStream){
            TCPSeekPacket.send(mOutputStream, seekTime);
        }
    }

    public void endSong(byte streamID){
        synchronized (mOutputStream){
            TCPEndSongPacket.send(mOutputStream, streamID);
        }
    }

    public void destroy(){
        mConnected = false;
        while(mThreadRunning){}
        mTransmitter = null;
        mMasterTCPHandler = null;
    }



}
