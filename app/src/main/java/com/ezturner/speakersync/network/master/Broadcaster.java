package com.ezturner.speakersync.network.master;

import android.os.Handler;
import android.util.Log;

import com.ezturner.speakersync.audio.AudioObserver;
import com.ezturner.speakersync.audio.AudioStatePublisher;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.network.TimeManager;
import com.ezturner.speakersync.network.master.transmitter.Transmitter;
import com.ezturner.speakersync.network.packets.FramePacket;
import com.ezturner.speakersync.network.packets.NetworkPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ethan on 2/8/2015.
 */
public class Broadcaster implements AudioObserver{

    public static final String LOG_TAG ="AudioBroadcaster";

    //The port that the stream will broadcast on
    public int mPort;

    //True if the listeners are running, false otherwise
    private boolean mStreamRunning;

    //Stream ID, so that we can tell when we get packets from an old stream
    private byte mStreamID;

    private Handler mHandler;

    //The ID of the packet to be sent next
    private int mNextFrameSendID;

    //The ID of the last frame in this stream
    private int mLastFrameID;

    //The boolean that lets us know if we are still broadcasting
    private boolean mIsBroadcasting;

    //the scheduler
    private ScheduledExecutorService mWorker;

    //The channels for the slave AudioTracks
    private int mChannels;

    private List<Integer> mPacketsToRebroadcast;

    private List<Slave> mSlaves;

    //The list of Audio Frames, exists for retransmission
    //TODO: get rid of old/unused ones
    private Map<Integer , AudioFrame> mFrames;

    //The class that handles all of the offset stuff
    private TimeManager mTimeManager;

    private boolean mSendRunnableRunning = false;
    private boolean mStartRunnableRunning = false;

    private boolean mEncodeDone = false;

    private AudioStatePublisher mAudioStatePublisher;

    //The Transmitters that transmit the audio data to their relevant destinations
    private List<Transmitter> mTransmitters;

    //Makes an AudioBroadcaster object
    //Creates the sockets, starts the NTP server and instantiates variables
    public Broadcaster(AudioTrackManager manager, AudioFileReader reader){
        mSlaves = new ArrayList<>();

        mTimeManager = TimeManager.getInstance();

        //set the stream ID to zero
        mStreamID = -1;

        //Makes the handler for broadcasting packets
        //TODO : delete if useless
        mHandler = new Handler();

        //Set the next packet to be created to be 0
        mNextFrameSendID = 0;
        mLastFrameID = -1;

        mIsBroadcasting = false;
        mPacketsToRebroadcast = new ArrayList<>();

        mStreamRunning = false;

        mEncodeDone = false;

        mWorker = Executors.newSingleThreadScheduledExecutor();

        mFrames = new TreeMap<>();

        mWorker.schedule(mSongStreamStart, 5000, TimeUnit.MILLISECONDS);

        //Get the AudioStatePublisher and then add ourselves to it
        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mAudioStatePublisher.attach(this);
    }

    //TODO: put in some code that will ensure that we haven't gotten off track due to rounding errors or  something like that

    private int mPacketsSentCount = 0;


    Runnable mSongStreamStart = new Runnable() {
        @Override
        public void run() {
            mStartRunnableRunning = true;
            Log.d(LOG_TAG, "Stream starting!");
            startSongStream();
            mStartRunnableRunning = false;
        }
    };


    //Starts streaming the song, starts the reliability listeners, and starts the control listener
    public void startSongStream(){

        //TODO: check if we're on a LAN or if we're on a mobile connection
        //TODO: start LANTransmitter.

        //Stop the old handler
        mHandler.removeCallbacks(mPacketSender);
        //If another stream is running,
        if(mStreamRunning){
            //TODO: fix this code so it works
            //mReliabilityListener.stop();
        }

        //The start time in milliseconds
        //TODO: Recalculate this!
        mTimeManager.setSongStartTime(System.currentTimeMillis() + 2500 + mTimeManager.getOffset());


        mFrames = new TreeMap<>();

        mStreamRunning = true;
        mEncodeDone = false;

        mNextFrameSendID = 0;

        if(mStreamID >= 254){
            mStreamID = 0;
        } else {
            mStreamID++;
        }

        mIsBroadcasting = true;
        Log.d(LOG_TAG, "Schedule task time!");
        mWorker.schedule(mPacketSender, 500 , TimeUnit.MILLISECONDS);
    }

    public void lastPacket(){
        Log.d(LOG_TAG , "mLastFrameID is : " + mLastFrameID);
        mEncodeDone = true;

    }

    public void rebroadcastPacket(int packetID){
        if(mPacketsToRebroadcast.contains(packetID)){
            return;
        }

        synchronized (mPacketsToRebroadcast){
            mPacketsToRebroadcast.add(packetID);
        }
    }

    public boolean isStreamRunning(){
        return mIsBroadcasting;
    }

    public int getPort(){
        return mPort;
    }

    private FramePacket createFramePacket(AudioFrame frame){
        FramePacket fp = new FramePacket(frame ,getStreamID() , frame.getID());
        if(fp == null){
            Log.d(LOG_TAG , "Frame Packet for frame #" + frame.getID() + " is null");
        }
        return fp;
    }
    /*Not in use atm

    //Makes the song Start packet, currently only has the songs's start time but will soon have more
    private SongStartPacket createStartSongPacket(){

        SongStartPacket songStartPacket = new SongStartPacket(mSongStartTime , mStreamID , 0 , mChannels);

        synchronized (mPackets){
            mPackets.remove(0);
            mPackets.add(0 , songStartPacket);
        }

        return songStartPacket;
    }*/


    public byte getStreamID(){
        return mStreamID;
    }

    public void addFrames(ArrayList<AudioFrame> frames){
        synchronized (mFrames){
            for(AudioFrame frame : frames){
                if(frame == null) Log.d(LOG_TAG , "Input AudioFrame # " +(mLastFrameID + 1) +" is null!");
                mFrames.put(frame.getID(), frame);
                mLastFrameID = frame.getID();
            }
        }

//        mMasterFECHandler.addFrames(frames);
    }

    public void setAudioInfo(int channels){
        mChannels = channels;
    }

    public List<Slave> getSlaves(){
        return mSlaves;
    }

    //The functions for adding and removing slaves
    public void addSlave(Slave slave){
        mSlaves.add(slave);
    }

    public void removeSlave(Slave slave){
        if(mSlaves.contains(slave)){
            mSlaves.remove(slave);
        }
    }
    public int getNextPacketSendID(){
        return mNextFrameSendID;
    }

    public int getChannels(){
        return mChannels;
    }


    public AudioFrame getFrame(int ID){
        synchronized (mFrames){
            return mFrames.get(ID);
        }
    }

    public void resume(long resumeTime){
        long newSongStartTime = System.currentTimeMillis() - resumeTime + 1000 + mTimeManager.getOffset();
        Log.d(LOG_TAG , "Resume time is : " + resumeTime + " and newSongStartTime is : " + newSongStartTime);
        mTimeManager.setSongStartTime(newSongStartTime);

        mTCPHandler.resume(resumeTime, newSongStartTime);
    }

    private boolean mSeek = false;
    public void seek(long seekTime){
        mSeek = true;
        mTCPHandler.seek(seekTime);
        mNextFrameSendID = (int)(seekTime / (1024000.0 / 44100.0));
    }

    public void destroy(){
        mAudioTrackManager = null;

        mTCPHandler.destroy();
        mTCPHandler = null;

        if(mStartRunnableRunning || mSendRunnableRunning){
            while (mSendRunnableRunning || mStartRunnableRunning){
                synchronized (this){
                    try{
                        this.wait(1);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }

        synchronized (mHandler) {
            mHandler.removeCallbacks(mPacketSender);
            mHandler.removeCallbacks(mSongStreamStart);
        }

        mDiscoveryHandler.destroy();
        mDiscoveryHandler = null;
    }

    public void stopStream(){
        mIsBroadcasting = false;
    }

    //TODO implement a way to keep the buffer at a certain amount (say 1000ms)
    //TODO: implement and calculate this based on the bitrate and whatnot for the FEC
    private long getDelay(){
        return 20;
    }

    public void retransmit(){
        mTCPHandler.retransmit();
    }

    // The function that receives updates from the AudioStatePublisher
    // This function is how we know what the user is doing in regards to pause/skip/resume
    @Override
    public void update(int state){
        switch (state){

            case AudioStatePublisher.IDLE:
                stopStream();
                break;

            case AudioStatePublisher.RESUME:
                long resumeTime = mAudioStatePublisher.getResumeTime();
                resume(resumeTime);
                break;

            case AudioStatePublisher.PAUSED:
                break;

            case AudioStatePublisher.SKIPPING:
                long seekTime = mAudioStatePublisher.getSeekTime();
                seek(seekTime);
                resume(mAudioStatePublisher.getResumeTime());
                break;
        }
    }

}