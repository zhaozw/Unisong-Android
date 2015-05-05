package com.ezturner.speakersync.network.master;

import android.os.Handler;
import android.util.Log;

import com.ezturner.speakersync.MediaService;
import com.ezturner.speakersync.audio.AudioFileReader;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.ntp.NtpServer;
import com.ezturner.speakersync.network.ntp.SntpClient;
import com.ezturner.speakersync.network.packets.AudioDataPacket;
import com.ezturner.speakersync.network.packets.FramePacket;
import com.ezturner.speakersync.network.packets.NetworkPacket;
import com.ezturner.speakersync.network.packets.SongStartPacket;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;
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

    public int MAX_PACKET_SIZE = 2048;

    //The IP that the broadcast stream will be sent on
    private InetAddress mStreamIP;

    //The multicast listener for giving out the IP of the multicast stream
    private MulticastSocket mControlSocket;

    //The multicast listener for giving out the IP of the multicast stream
    private DatagramSocket mStreamSocket;

    //True if the listeners are running, false otherwise
    private boolean mStreamRunning;

    //Random object, used to randomize multicast stream IP
    static private Random random = new Random();

    //ArrayList of packets made
    private ArrayList<NetworkPacket> mPackets;

    //The object that handles all reliability stuff
    private MasterReliabilityHandler mReliabilityHandlder;

    //Stream ID, so that we can tell when we get packets from an old stream
    private byte mStreamID;

    //Handles the network discovery
    private MasterDiscoveryHandler mDiscoveryHandler;

    //In microseconds , the length of a frame
    private long mFrameLength;

    //The time at which the next packet should be played
    private long mNextFrameTime;

    //The time at which the current song either has or will start
    private long mSongStartTime;

    //The current file being read from
    private File mCurrentFile;

    private Handler mHandler;

    //The ID of the packet to be sent next
    private int mNextPacketSendID;

    //The ID of the next packet to be made
    private int mNextPacketCreateID;

    //The ID of the last packet in this stream
    private int mLastPacketID;

    private int mLastFrameID;

    //The AudioTrackManager that handles the playback of the audio data on this device
    private AudioTrackManager mManager;

    //The object that handles the reading and decoding of all of dem music
    private AudioFileReader mReader;

    //The boolean that lets us know if we are still broadcasting
    private boolean mIsBroadcasting;

    //The NTP server for time synchronization
    private NtpServer mNtpServer;

    //the scheduler
    private ScheduledExecutorService mWorker;

    //The time that the packets started sending at
    private long mPacketSendStartTime;

    //The sample rate for the AudioTracks on the slave devices
    private int mSampleRate;

    //The channels for the slave AudioTracks
    private int mChannels;

    //The packet that will be used as the song starter
    private SongStartPacket mSongStartPacket;

    //The mime type
    private String mMime;

    //The bitrate of the current song
    private int mBitrate;

    //the duration
    private long mDuration;

    private List<Integer> mPacketsToRebroadcast;

    //The thread that will read in the file to be broadcast.
    private Thread mReadThread;

    private List<Slave> mSlaves;

    private SntpClient mSntpClient;

    //The time offset as calculated by SntpClient from pool.ntp.org
    private long mOffset;


    //Makes an AudioBroadcaster object
    //Creates the sockets, starts the NTP server and instantiates variables
    public AudioBroadcaster(AudioTrackManager manager , AudioFileReader reader , SntpClient client){

        mSlaves = new ArrayList<>();
        mSntpClient = client;

        if(mSntpClient.hasOffset()){
            mOffset = (long) mSntpClient.getOffset();
        }

        mSntpClient.setBroadcaster(this);

        //TODO: When not testing, get rid of this comment
        mPort = CONSTANTS.STREAM_PORT_BASE;// + random.nextInt(PORT_RANGE);
        //TODO: Listen for other streams and ensure that you don't use the same port
        try {
            mStreamIP = NetworkUtilities.getBroadcastAddress();
            //Start the socket for the actual stream
            mStreamSocket = new DatagramSocket();

            mDiscoveryHandler = new MasterDiscoveryHandler(this);
            mReliabilityHandlder = new MasterReliabilityHandler(this);

            //Start the NTP server for syncing the playback
            mNtpServer = new NtpServer();

        } catch(IOException e){
            e.printStackTrace();
        }

        mReader = reader;

        //set the stream ID to zero
        mStreamID = 0;

        //Make the map of the packets
        mPackets = new ArrayList<NetworkPacket>();

        //Makes the handler for broadcasting packets
        //TODO : delete if useless
        mHandler = new Handler();

        //Set the next packet to be created to 1, the song start packet is 0
        mNextPacketCreateID = 0;

        //Set the next packet to be created to be 0
        mNextPacketSendID = 0;
        mLastFrameID = -1;

        mPacketsToRebroadcast = new ArrayList<>();

        //TODO: fix and implement
        mIsBroadcasting = false;

        mManager = manager;

        mReader = reader;

        //TODO: Switch to false, just for the test
        mStreamRunning = true;

        mLastPacketID = -1;

        mWorker = Executors.newSingleThreadScheduledExecutor();

        mWorker.schedule(mSongStreamStart, 5000, TimeUnit.MILLISECONDS);
    }

    //TODO: put in some code that will ensure that we haven't gotten off track due to rounding errors or  something like that

    private int mPacketsSentCount = 0;

    Runnable mPacketSender = new Runnable() {
        @Override
        public void run() {


            long begin = System.currentTimeMillis();
            if(mNextPacketSendID == 0){
                mReliabilityHandlder.startSong(mSongStartTime, mChannels , mStreamID);
            }

            NetworkPacket packet;
            synchronized (mPackets){
                packet = mPackets.get(mNextPacketSendID);
                //Log.d(LOG_TAG , packet.toString());
                mNextPacketSendID++;
            }



            synchronized (mStreamSocket){
                try {
                    int len = packet.getPacket().getLength();
                    mStreamSocket.send(packet.getPacket());
                    /*
                    try{
                        synchronized (this){
                            this.wait(10);
                        }
                    }catch (InterruptedException e){

                    }
                    mStreamSocket.send(new DatagramPacket(new byte[len] , len));*/
                    mPacketsSentCount++;
                } catch (IOException e){
                    e.printStackTrace();
                }
            }


            //TODO: figure out a way to set delay properly
            //long delay = (long)(mPacketSendStartTime + packets.getInfoPacket().getFrame().getPlayTime()) - System.currentTimeMillis();
            long delay = 23;


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
                    Log.d(LOG_TAG, "packet to be rebroadcast is: " + packetl.toString());

                    for(Slave slave : mSlaves){
                        slave.packetHasBeenRebroadcasted(packetl.getPacketID());
                    }
                    mPacketsToRebroadcast.remove(0);

                    if(!mReliabilityHandlder.checkSlaves(packetl.getPacketID())) {
                        try {
                            mStreamSocket.send(packetl.getPacket());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        int packetID = mPacketsToRebroadcast.get(0);
                        while(mReliabilityHandlder.checkSlaves(packetID)){
                            mPacketsToRebroadcast.remove(0);
                            packetID = mPacketsToRebroadcast.get(0);
                        }
                    }
                }


            }
            delay -= System.currentTimeMillis() - begin;

            if(delay < 0){
                delay = 0;
            }

            if(mPacketsSentCount % 100 == 0) {

                Log.d(LOG_TAG, "mPacketsSentCount :" + mPacketsSentCount + " , delay is : " + delay);
            }


            if(mNextPacketSendID != mLastFrameID) {
                mWorker.schedule(mPacketSender , delay , TimeUnit.MILLISECONDS);
            }
        }

    };


    Runnable mSongStreamStart = new Runnable() {
        @Override
        public void run() {
            mStreamSocket.connect(mStreamIP , getPort());
            Log.d(LOG_TAG , "Stream starting!");
            startSongStream();
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
        mSongStartTime = System.currentTimeMillis() + 2000 + mOffset;
        mManager.startSong(mSongStartTime);
        mNextFrameTime = mSongStartTime;

        mNextPacketCreateID= 0;

        mNextPacketSendID = 0;
        mLastPacketID = 0;

        if(mStreamID == 240){
            mStreamID = 0;
        } else {
            mStreamID++;
        }

        mPackets = new ArrayList<NetworkPacket>();




        try{
            mReader.readFile(MediaService.TEST_FILE_PATH);
        } catch(IOException e){
            e.printStackTrace();
        }

        mIsBroadcasting = true;
        Log.d(LOG_TAG, "Schedule task time!");
        mWorker.schedule(mPacketSender, 500, TimeUnit.MILLISECONDS);
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

    public int getCurrentStreamID(){
        return mStreamID;
    }

    public int getPort(){
        return mPort;
    }

    // Returns the designated time for the next frame in microseconds and
    // increases the time for the next one
    public long getNextFrameWriteTime(){
        long time = mSongStartTime;
        mSongStartTime += mFrameLength;
        return time;
    }

    //Set the frame length in microseconds
    public void setFrameLength(long frameLength){
        mFrameLength = frameLength;
    }


    private void addAudioDataPacket(byte[] data){
        AudioDataPacket ap = new AudioDataPacket(data, mStreamID , mNextPacketCreateID);
        mPackets.add(ap);
        mNextPacketCreateID++;
    }

    private void createFramePacket(AudioFrame frame){
        FramePacket fp = new FramePacket(frame ,getStreamID() , mNextPacketCreateID);
        mNextPacketCreateID++;
        mPackets.add(fp);
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
    public void lastPacket(){
        mLastPacketID = mNextPacketCreateID;
    }


    public byte getStreamID(){
        return mStreamID;
    }

    public void addFrames(ArrayList<AudioFrame> frames){
        synchronized (mPackets){
            for(AudioFrame frame : frames){
                createFramePacket(frame);
            }
        }
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

    public void setOffset(double offset){
        Log.d(LOG_TAG , "Offset is : " + offset );
        mManager.setOffset(offset);
        mOffset = (long) offset;
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
}