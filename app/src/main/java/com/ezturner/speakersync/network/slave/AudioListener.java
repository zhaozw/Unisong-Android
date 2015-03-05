package com.ezturner.speakersync.network.slave;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.master.AudioBroadcaster;
import com.ezturner.speakersync.network.ntp.SntpClient;
import com.ezturner.speakersync.network.packets.FrameDataPacket;
import com.ezturner.speakersync.network.packets.FrameInfoPacket;
import com.ezturner.speakersync.network.packets.SongStartPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Ethan on 2/8/2015.
 * This class handles listening to and audio stream. Its component discovery handler handles
 * discovery, and the reliability handler handles reliability
 *
 */
public class AudioListener {


    //The boolean indicating whether the above objects are in use(a master has not been chosen yet)
    private boolean mIsDeciding;

    //The boolean indicating whether we are listening to a stream
    private boolean mIsListening;

    //The Sntp client for time synchronization
    private SntpClient mSntpClient;

    //The time offset between this client and the master
    private long mTimeOffset;

    //The multicast socket for discovery and control
    private MulticastSocket mManagementSocket;

    //the socket for receiving the stream
    private DatagramSocket mSocket;

    //The ArrayList of received packets
    private ArrayList<DatagramPacket> mPackets;

    //The master that the listener is currently listening to
    private Master mMaster;

    //The broadcast address that will be used
    private InetAddress mAddress;

    //The port that the stream will be on
    private int mPort;

    private AudioTrackManager mAudioTrackManager;

    //The discovery handler, which will handle finding and choosing the
    private SlaveDiscoveryHandler mSlaveDiscoveryHandler;

    //The Slave reliability handler which handles packet reliability
    private SlaveReliabilityHandler mSlaveReliabilityHandler;

    //The activity context
    private Context mContext;

    //The unfinished AudioFrames that need to be built and sent over to the AudioTrackManager
    private Map<Integer , AudioFrame> mUnfinishedFrames;

    //The stream ID
    private byte mStreamId;


    public AudioListener(Context context , AudioTrackManager atm){

        Log.d("ezturner" , "Audio Listener Started");
        mAudioTrackManager = atm;
        mContext = context;

        mSlaveDiscoveryHandler = new SlaveDiscoveryHandler(this , mContext);

        mAddress = NetworkUtilities.getBroadcastAddress();

        mIsListening = false;


    }

    //Start playing from a master, start listening to the stream
    public void playFromMaster(Master master , ArrayList<DatagramPacket> packets, SntpClient client){
        mSntpClient = client;
        mPackets = packets;
        mMaster = master;

        mPort = master.getPort();

        mSocket = master.getSocket();

        mSlaveReliabilityHandler = new SlaveReliabilityHandler(master.getIP());

        mUnfinishedFrames = new HashMap<Integer , AudioFrame>();
    }

    //Starts the process of finding masters
    public void findMasters() {
        mSlaveDiscoveryHandler.findMasters();
    }

    private Thread startListeningForPackets(){
        return new Thread(){
            public void run(){
                while(mIsListening){

                }
            }
        };
    }

    private void listenForPacket(){
        DatagramPacket packet = new DatagramPacket(new byte[1536] , 1536);

        try{
            mSocket.receive(packet);
        } catch(IOException e){
            e.printStackTrace();
        }

        if(packet.getData()[1] == mStreamId) {
            switch (packet.getData()[0]) {
                case CONSTANTS.FRAME_INFO_PACKET_ID:
                    handleFrameInfoPacket(packet);
                    break;

                case CONSTANTS.FRAME_DATA_PACKET_ID:
                    handleFrameDataPacket(packet);
                    break;

                case CONSTANTS.SONG_START_PACKET_ID:
                    handleSongSwitch(packet);
                    break;
            }
        }
    }


    public long getNextFrameWriteTime(){
        return 0;
    }

    private void handleFrameInfoPacket(DatagramPacket packet){
        FrameInfoPacket fp = new FrameInfoPacket(packet.getData());

        AudioFrame frame = new AudioFrame(fp.getFrameId() , fp.getNumPackets() , fp.getPlayTime() , fp.getLength(), fp.getPacketId() );

        mUnfinishedFrames.put(frame.getID() , frame);
    }

    private void handleFrameDataPacket(DatagramPacket packet){
        FrameDataPacket fp = new FrameDataPacket(packet.getData());

        AudioFrame frame = mUnfinishedFrames.get(fp.getFrameID());

        boolean frameFinished = frame.addData(fp.getFrameID() , fp.getAudioFrameData());

        if(frameFinished){
            mAudioTrackManager.addFrame(frame);
            mUnfinishedFrames.remove(fp.getFrameID());
        }
    }

    private void handleSongSwitch(DatagramPacket packet){
        SongStartPacket sp = new SongStartPacket(packet.getData());

        mAudioTrackManager.release();
    }

}
