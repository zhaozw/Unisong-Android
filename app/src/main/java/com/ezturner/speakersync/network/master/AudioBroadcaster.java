package com.ezturner.speakersync.network.master;

import android.os.Handler;
import android.util.Log;

import com.ezturner.speakersync.MediaService;
import com.ezturner.speakersync.audio.master.AudioFileReader;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.network.AnalyticsSuite;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.TimeManager;
import com.ezturner.speakersync.network.packets.FramePacket;
import com.ezturner.speakersync.network.packets.NetworkPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ethan on 2/8/2015.
 */
public class AudioBroadcaster {

    public static final String LOG_TAG ="AudioBroadcaster";

    //The port that the stream will broadcast on
    public int mPort;

    //The IP that the broadcast stream will be sent on
    private InetAddress mStreamIP;

    //The multicast listener for giving out the IP of the multicast stream
    private DatagramSocket mStreamSocket;

    //True if the listeners are running, false otherwise
    private boolean mStreamRunning;

    //Random object, used to randomize multicast stream IP
    static private Random random = new Random();

    //ArrayList of packets made
    private Map<Integer , NetworkPacket> mPackets;

    //The object that handles all reliability stuff
    private MasterTCPHandler mTCPHandlder;

    //Stream ID, so that we can tell when we get packets from an old stream
    private byte mStreamID;

    //Handles the network discovery
    private MasterDiscoveryHandler mDiscoveryHandler;

    //In microseconds , the length of a frame
    private long mFrameLength;

    //The time at which the current song either has or will start
    private long mSongStartTime;

    private Handler mHandler;

    //The ID of the packet to be sent next
    private int mNextPacketSendID;

    //The ID of the last frame in this stream
    private int mLastFrameID;

    //The AudioTrackManager that handles the playback of the audio data on this device
    private AudioTrackManager mAudioTrackManager;

    //The object that handles the reading and decoding of all of dem music
    private AudioFileReader mReader;

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
    private List<AudioFrame> mFrames;

    //The class that handles all of the offset stuff
    private TimeManager mTimeManager;

    private boolean mSendRunnableRunning = false;
    private boolean mStartRunnableRunning = false;

    private MasterFECHandler mMasterFECHandler;

    //The Analytics Suite that I use to help me get information about data transfer and whatnot
    private AnalyticsSuite mAnalyticsSuite;

    private boolean mEncodeDone = false;

    //Makes an AudioBroadcaster object
    //Creates the sockets, starts the NTP server and instantiates variables
    public AudioBroadcaster(AudioTrackManager manager , AudioFileReader reader , TimeManager timeManager , AnalyticsSuite analyticsSuite){

        mAnalyticsSuite = analyticsSuite;
        mSlaves = new ArrayList<>();

        mTimeManager = timeManager;

        //TODO: When not testing, get rid of this comment
        mPort = CONSTANTS.STREAM_PORT_BASE;// + random.nextInt(PORT_RANGE);
        //TODO: Listen for other streams and ensure that you don't use the same port
        try {
            mStreamIP = NetworkUtilities.getBroadcastAddress();
            //Start the socket for the actual stream
            mStreamSocket = new DatagramSocket();

            mDiscoveryHandler = new MasterDiscoveryHandler(this);
            mTCPHandlder = new MasterTCPHandler(this , mAnalyticsSuite);


        } catch(IOException e){
            e.printStackTrace();
        }

        mMasterFECHandler = new MasterFECHandler(this);

        mReader = reader;

        //set the stream ID to zero
        mStreamID = -1;

        //Make the map of the packets
        mPackets = new HashMap<>();

                //Makes the handler for broadcasting packets
        //TODO : delete if useless
        mHandler = new Handler();

        //Set the next packet to be created to be 0
        mNextPacketSendID = 0;
        mLastFrameID = -1;

        mPacketsToRebroadcast = new ArrayList<>();

        mAudioTrackManager = manager;

        mReader = reader;

        mStreamRunning = false;

        mEncodeDone = false;

        mWorker = Executors.newSingleThreadScheduledExecutor();

        mFrames = new ArrayList<>();

        mWorker.schedule(mSongStreamStart, 5000, TimeUnit.MILLISECONDS);
    }

    //TODO: put in some code that will ensure that we haven't gotten off track due to rounding errors or  something like that

    private int mPacketsSentCount = 0;

    Runnable mPacketSender = new Runnable() {
        @Override
        public void run() {


            mSendRunnableRunning = true;
            long begin = System.currentTimeMillis();

//            Log.d(LOG_TAG , "Starting packet send!");
            NetworkPacket packet;
            synchronized (mPackets){
                packet = mPackets.get(mNextPacketSendID);
                if(packet == null){
                    Log.d(LOG_TAG , "Packet #" + mNextPacketSendID + " is null! mPackets size is: " + mPackets.size());
//                    if(mPackets.containsKey(mNextPacketSendID + 1)){
//                        packet = mPackets.get(mNextPacketSendID + 1);
//                        mNextPacketSendID++;
//                    }
                }
                //Log.d(LOG_TAG , packet.toString());
            }

            if(packet == null){
                while (!mPackets.containsKey(mNextPacketSendID)){
                    synchronized (this){
                        try {
                            this.wait(1);
                        } catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                }
                synchronized (mPackets) {
                    packet = mPackets.get(mNextPacketSendID);
                }
                Log.d(LOG_TAG  ,"Packet #" + mNextPacketSendID + " found.");
                if(packet == null){
                    Log.e(LOG_TAG , "Packet is still null!!!!");
                }
            }

            mNextPacketSendID++;



//            Log.d(LOG_TAG , "Sending packet!");
            synchronized (mStreamSocket){
                try {
                    DatagramPacket datagramPacket = packet.getPacket();

                    if(datagramPacket == null){
                        Log.d(LOG_TAG , "The datagram packet is null for packet #" + (mNextPacketSendID -1));
                    }

                    if(!mStreamRunning){
                        return;
                    }

                    mAnalyticsSuite.packetSent(packet.getPacketID());
                    mStreamSocket.send(datagramPacket);

                    mPacketsSentCount++;
                } catch (IOException e){
                    e.printStackTrace();
                }
            }

//
//            Log.d(LOG_TAG , "Packet Sent!");
            long delay = getDelay();


            rebroadcast();
            delay -= System.currentTimeMillis() - begin;

            if(delay < 0){
                delay = 0;
            }

            long diff = System.currentTimeMillis() - mTimeManager.getAACPlayTime(packet.getPacketID());
//            Log.d(LOG_TAG , "For Packet #" + packet.getPacketID() + " , the difference between now and play time is : " + diff + "ms" );
            if(mPacketsSentCount % 100 == 0) {

                Log.d(LOG_TAG, "mPacketsSentCount :" + mPacketsSentCount + " , delay is : " + delay);
            }


            if(mNextPacketSendID != mLastFrameID || !mEncodeDone) {
                mWorker.schedule(mPacketSender , delay , TimeUnit.MILLISECONDS);
            }
            mSendRunnableRunning = false;
        }

    };


    Runnable mSongStreamStart = new Runnable() {
        @Override
        public void run() {
            mStartRunnableRunning = true;
            mStreamSocket.connect(mStreamIP , getPort());
            Log.d(LOG_TAG, "Stream starting!");
            startSongStream();
            mStartRunnableRunning = false;
        }
    };


    //Starts streaming the song, starts the reliability listeners, and starts the control listener
    public void startSongStream(){

        //Stop the old handler
        mHandler.removeCallbacks(mPacketSender);
        //If another stream is running,
        if(mStreamRunning){
            //TODO: fix this code so it works
            //mReliabilityListener.stop();
        }

        //The start time in milliseconds
        //TODO: Recalculate this!
        mSongStartTime = System.currentTimeMillis() + 1500 + mTimeManager.getOffset();
        mTimeManager.setSongStartTime(mSongStartTime);
        mAudioTrackManager.startSong(mSongStartTime);

        mStreamRunning = true;
        mEncodeDone = false;

        mNextPacketSendID = 0;

        if(mStreamID == 240){
            mStreamID = 0;
        } else {
            mStreamID++;
        }

        mPackets = new HashMap<>();

        try{
            mReader.readFile(MediaService.TEST_FILE_PATH);
        } catch(IOException e){
            e.printStackTrace();
        }

        mTCPHandlder.startSong(mSongStartTime, mChannels, mStreamID);
        mIsBroadcasting = true;
        Log.d(LOG_TAG, "Schedule task time!");
        mWorker.schedule(mPacketSender, 500 , TimeUnit.MILLISECONDS);
    }

    //Broadcasts a streaming packet
    private boolean broadcastStreamPacket(int packetID){
        boolean contains;
        NetworkPacket packet;
        synchronized(mPackets){
            contains = mPackets.size() <= packetID;
            if(contains){
                packet = mPackets.get(packetID);
            }
        }

        if(contains){
            try {
                Log.d(LOG_TAG , "Packet to be sent is: " + packetID);
                synchronized (mStreamSocket){
                    mStreamSocket.send(mPackets.get(packetID).getPacket());
                }
                return true;
            } catch (IOException e){
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public void lastPacket(){
        mEncodeDone = true;
        mTCPHandlder.lastPacket(mLastFrameID);
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

    private void createFramePacket(AudioFrame frame){
        FramePacket fp = new FramePacket(frame ,getStreamID() , frame.getID());
        if(fp == null){
            Log.d(LOG_TAG , "Frame Packet for frame #" + frame.getID() + " is null");
        }
        mPackets.put(fp.getPacketID(), fp);
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
        synchronized (mPackets){
            for(AudioFrame frame : frames){
                if(frame == null) Log.d(LOG_TAG , "Input AudioFrame # " +(mLastFrameID + 1) +" is null!");
                createFramePacket(frame);
                mFrames.add(frame);
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
        return mNextPacketSendID;
    }

    public int getChannels(){
        return mChannels;
    }

    public long getSongStartTime(){
        return mSongStartTime;
    }


    private void rebroadcast(){
        if(mPacketsToRebroadcast.size() > 0) {
            try{
                synchronized (this){
                    this.wait(10);
                }
            }catch (InterruptedException e){

            }

            synchronized (mPacketsToRebroadcast){
                NetworkPacket packetl= null;
                synchronized (mPackets) {
                    packetl = mPackets.get(mPacketsToRebroadcast.get(0));
                }

                if(packetl == null){
                    Log.d(LOG_TAG , "Packet to be rebroadcast is null! #" + mPacketsToRebroadcast.get(0));
                }
                Log.d(LOG_TAG, "packet to be rebroadcast is: " + packetl.toString());

                int count = 0;
                Slave oneSlave = null;
                for(Slave slave : mSlaves ){
                    if(!slave.hasPacket(packetl.getPacketID())){
                        count++;
                        oneSlave = slave;
                    }
                }

                if(count == 0){
                    synchronized (mPacketsToRebroadcast) {
                        mPacketsToRebroadcast.remove(0);
                    }
                    rebroadcast();
                    return;
                } else if(count == 1){
//                    TODO: comment for local rebroadcasting tests
                    mTCPHandlder.sendFrameTCP(((FramePacket)packetl).getFrame(), oneSlave);
                    synchronized (mPacketsToRebroadcast) {
                        mPacketsToRebroadcast.remove(0);
                    }

                    rebroadcast();
                    return;
                }

                for(Slave slave : mSlaves){
                    slave.packetHasBeenRebroadcasted(packetl.getPacketID());
                }

                for(Slave slave : mSlaves){
                    List<Integer> packets = slave.getPacketsToBeReSent();
                    for(Integer i : packets) {
                        if (!mPacketsToRebroadcast.contains(i)) {
                            mPacketsToRebroadcast.add(i);
                        }
                    }
                }

                mPacketsToRebroadcast.remove(0);

                if(!mTCPHandlder.checkSlaves(packetl.getPacketID())) {
                    try {
                        mStreamSocket.send(packetl.getPacket());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    int packetID = mPacketsToRebroadcast.get(0);
                    while(mTCPHandlder.checkSlaves(packetID)){
                        mPacketsToRebroadcast.remove(0);
                        packetID = mPacketsToRebroadcast.get(0);
                    }
                }
            }


        }
    }

    public AudioFrame getFrame(int ID){
        synchronized (mFrames){
            return mFrames.get(ID);
        }
    }

    public void addPacket(NetworkPacket packet){
        mPackets.put(packet.getPacketID() , packet);
    }

    public void pause(){
        mTCPHandlder.pause();
    }

    public void resume(long resumeTime){
        long newSongStartTime = System.currentTimeMillis() - resumeTime + 1000 + mTimeManager.getOffset();
        Log.d(LOG_TAG , "Resume time is : " + resumeTime + " and newSongStartTime is : " + newSongStartTime);
        mTimeManager.setSongStartTime(newSongStartTime);

        mTCPHandlder.resume(resumeTime, newSongStartTime);
    }

    private boolean mSeek = false;
    public void seek(long seekTime){
        mSeek = true;
        mTCPHandlder.seek(seekTime);
        mNextPacketSendID = (int)(seekTime / (1024000.0 / 44100.0));
    }

    public void destroy(){
        mAudioTrackManager = null;

        mTCPHandlder.destroy();
        mTCPHandlder = null;

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

        mMasterFECHandler.destroy();
        mMasterFECHandler = null;
    }

    //TODO implement a way to keep the buffer at a certain amount (say 1000ms)
    //TODO: implement and calculate this based on the bitrate and whatnot for the FEC
    private long getDelay(){
        return 20;
    }

    public void retransmit(){
        mTCPHandlder.retransmit();
    }
}