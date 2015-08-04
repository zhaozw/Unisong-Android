package com.ezturner.speakersync.network.client;

import android.content.Context;
import android.util.Log;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioObserver;
import com.ezturner.speakersync.audio.AudioStatePublisher;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.slave.SlaveDecoder;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.TimeManager;
import com.ezturner.speakersync.network.client.receiver.LANReceiver;
import com.ezturner.speakersync.network.packets.FramePacket;
import com.ezturner.speakersync.network.packets.NetworkPacket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;


/**
 * Created by Ethan on 2/8/2015.
 * This class handles listening to and audio stream. Its component discovery handler handles
 * discovery, and the reliability handler handles reliability
 *
 */
public class Listener implements AudioObserver{


    private final static String LOG_TAG = "Listener";

    //The discovery handler, which will handle finding and choosing the
    private ClientDiscoveryHandler mSlaveDiscoveryHandler;

    //The Client reliability handler which handles packet reliability
    private ClientTCPHandler mClientTCPHandler;

    //The activity context
    private Context mContext;

    //The stream ID
    private byte mStreamID;

    private SlaveDecoder mSlaveDecoder;

    //The class that handles all of the time operations
    private TimeManager mTimeManager;

    //The class that tells us what the current audio state is
    private AudioStatePublisher mAudioStatePublisher;

    //The bridge that handles communication between this class and the slave decoder and audio manager
    //TODO : get rid of this.
    private ListenerBridge mBridge;

    private AudioTrackManager mManager;

    private LANReceiver mLANReceiver;

    //TODO: when receiving from the server, hold on to the AAC data just in case we do a skip backwards to save on bandwidth and battery.
    public Listener(){

        mManager = AudioTrackManager.getInstance();

        mTimeManager = TimeManager.getInstance();

        mSlaveDecoder = new SlaveDecoder(2, (byte)0);

        mBridge = new ListenerBridge(mSlaveDecoder , mManager);

        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mAudioStatePublisher.attach(this);

        Log.d(LOG_TAG, "Audio Listener Started");

        mSlaveDiscoveryHandler = new ClientDiscoveryHandler(this);

    }


    //Start playing from a master, start listening to the stream
    public void playFromMaster(Master master){

        mStartSongReceived = false;
        mClientTCPHandler = new ClientTCPHandler(master.getIP() , master.getPort() , this );

        if(mLANReceiver != null){
            mLANReceiver.playFromMaster(master);
        }
    }

    public void packetReceived(int packetID){
        mClientTCPHandler.packetReceived(packetID);
    }

    //Starts the process of finding masters
    public void findMasters() {
        mSlaveDiscoveryHandler.findMasters();
    }





    private boolean songStarted = false;

    public void startSong(long startTime , int channels , byte streamID , int currentPacket){

        //TODO: get rid of this, it's just test code
        if(!songStarted){
            songStarted = true;
        } else {
            Log.d(LOG_TAG , "Song is being started a second time!");
        }

        mTimeManager.setSongStartTime(startTime);
        mStartSongReceived = true;
        mStreamID = streamID;

        mStartSongReceived = true;

        mBridge.startSong(startTime, currentPacket);
    }


    private boolean mStartSongReceived = false;


    public void setTrackBridge(ListenerBridge bridge){
        mBridge = bridge;
    }

    public DatagramPacket getPacket(int ID){
        return mLANReceiver.getPacket(ID);
    }

    public void addFrame(AudioFrame frame){
        mBridge.addFrame(frame);
    }

    public void resume(long resumeTime , long newSongStartTime){
        mTimeManager.setSongStartTime(newSongStartTime);
//        mAudioStatePublisher.
    }

    //TODO: implement this
    //Ends the current song, either in preparation for another or not
    public void endSong(){

    }

    public synchronized void destroy() {
        mClientTCPHandler.destroy();
        mSlaveDiscoveryHandler.destroy();
        mSlaveDecoder.destroy();

        if(mLANReceiver != null){
            mLANReceiver.destroy();
        }

        mBridge.destroy();
    }

    @Override
    public void update(int state){

    }
}
