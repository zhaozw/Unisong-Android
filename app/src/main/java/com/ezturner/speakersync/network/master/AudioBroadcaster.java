package com.ezturner.speakersync.network.master;

import android.net.Network;
import android.provider.MediaStore;
import android.util.Log;
import android.os.Handler;

import com.ezturner.speakersync.MediaService;
import com.ezturner.speakersync.audio.AudioFileReader;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.ntp.NtpServer;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.packets.FrameDataPacket;
import com.ezturner.speakersync.network.packets.FrameInfoPacket;
import com.ezturner.speakersync.network.packets.FramePacket;
import com.ezturner.speakersync.network.packets.MimePacket;
import com.ezturner.speakersync.network.packets.NetworkPacket;
import com.ezturner.speakersync.network.packets.SongStartPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
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

    //The packets of all of the frames.
    private ArrayList<FramePackets> mFramePackets;

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


    private Handler mHandler;

    //The ID of the packet to be sent next
    private int mNextFrameID;

    //The ID of the next packet to be made
    private int mNextPacketID;

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


    //Makes an AudioBroadcaster object
    //Creates the sockets, starts the NTP server and instantiates variables
    public AudioBroadcaster(AudioTrackManager manager , AudioFileReader reader){

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

        //set the stream ID to zero
        mStreamID = 0;

        //Make the map of the packets
        mPackets = new ArrayList<NetworkPacket>();

        //Makes the handler for broadcasting packets
        //TODO : delete if useless
        mHandler = new Handler();

        //Set the next packet to be created to 1, the song start packet is 0
        mNextPacketID = 0;

        //Set the next packet to be created to be 0
        mNextFrameID = 0;
        mLastFrameID = -1;

        mPacketsToRebroadcast = new ArrayList<>();

        //TODO: fix and implement
        mIsBroadcasting = true;

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

            synchronized (mPackets) {
                if (mNextFrameID == 0) {
                    NetworkPacket songStartPacket = mPackets.get(mNextFrameID);
                    mNextFrameID++;
                    NetworkPacket mimePacket = mPackets.get(mNextFrameID);
                    mNextFrameID++;
                    try {
                        mStreamSocket.send(songStartPacket.getPacket());
                        mStreamSocket.send(mimePacket.getPacket());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            FramePacket packet;
            synchronized (mPackets){
                packet =(FramePacket) mPackets.get(mNextFrameID);
                mNextFrameID++;
            }


            synchronized (mStreamSocket){
                try {
                    mStreamSocket.send(packet.getPacket());
                    mPacketsSentCount++;
                } catch (IOException e){
                    e.printStackTrace();
                }
            }

            /*
            FramePackets packets;
            synchronized(mFramePackets) {
                packets = mFramePackets.get(mNextFrameID);
            }*/

            //long delay = (long)(mPacketSendStartTime + packets.getInfoPacket().getFrame().getPlayTime()) - System.currentTimeMillis();
            long delay = packet.getLength() / 1000;

            if(mPacketsToRebroadcast.size() > 0) {
                synchronized (mPacketsToRebroadcast) {
                    ArrayList<NetworkPacket> rebroadcastPackets = new ArrayList<>();
                    synchronized (mPackets) {
                        for (int i = 0; i < mPacketsToRebroadcast.size(); i++) {
                            int id = mPacketsToRebroadcast.get(i);
                            rebroadcastPackets.add(mPackets.get(id));
                        }
                    }

                    for (int i = 0; i < 4; i++) {
                        try {
                            NetworkPacket packetl = mPackets.get(mPacketsToRebroadcast.get(i));
                            mStreamSocket.send(packetl.getPacket());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    for(int i = 0; i < 4; i+= 0) {
                        mPacketsToRebroadcast.remove(i);
                    }
                }
            }

            /*
            synchronized(mStreamSocket){
                DatagramPacket packet = packets.getInfoPacket().getPacket();
                try {
                    mStreamSocket.send(packet);
                    mPacketsSentCount++;
                } catch (IOException e){
                    e.printStackTrace();
                }

                for(FrameDataPacket fdPacket : packets.getDataPackets()){
                    packet = fdPacket.getPacket();

                    try {
                        mStreamSocket.send(packet);
                        mPacketsSentCount++;
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }*/

//            Log.d(LOG_TAG , "mPacketsSentCount :" + mPacketsSentCount + " , delay is : " + delay);


            if(mNextFrameID != mLastFrameID) {
                //mWorker.schedule(mPacketSender , delay , TimeUnit.MILLISECONDS);
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
        mSongStartTime = System.currentTimeMillis() + 1000;
        mManager.startSong(mSongStartTime);
        mNextFrameTime = mSongStartTime;

        mNextPacketID = 0;

        mNextFrameID = 0;
        mLastPacketID = 0;

        if(mStreamID == 240){
            mStreamID = 0;
        } else {
            mStreamID++;
        }

        mPackets = new ArrayList<NetworkPacket>();
        mFramePackets = new ArrayList<FramePackets>();

        try {
            mReader.readFile(MediaService.TEST_FILE_PATH);
        } catch(IOException e){
            e.printStackTrace();
        }

        mIsBroadcasting = true;
        Log.d(LOG_TAG , "Schedule task time!");
        mWorker.schedule(mPacketSender, 500, TimeUnit.MILLISECONDS);
        mPacketSendStartTime = System.currentTimeMillis() + 500;
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

    //Creates frames of 1024 bytes in length, and saves any leftover
    //data in mLeftoverBytes
    public void handleFrame(AudioFrame frame){

        addFramePacket(frame);
        return;
        /*
        byte[] data = frame.getData();

        int frameID = frame.getID();

        ArrayList<byte[]> datas = new ArrayList<byte[]>();

        int bytesLeft = data.length;
        //the position we are at in the data array
        int dataPos = 0;
        while(bytesLeft > 0){

            //If we have enough data to make a packet, then lets make one
            if(bytesLeft >= 1024){
                byte[] frameData = new byte[1024];

                for(int i = 0; i < 1024; i++){
                    frameData[i] = data[dataPos + i];
                }
                //Change the indexes
                dataPos += 1024;
                bytesLeft -= 1024;

                //Add the data to the ArrayList of byte[]
                datas.add(frameData);
            } else {
                //Otherwise, just put it in mLeftoverBytes and save it for the next call
                byte[] leftoverData = new byte[bytesLeft];

                for(int i = 0; i < bytesLeft; i++){
                    leftoverData[i] = data[i + dataPos];
                }
                bytesLeft = 0;

                datas.add(leftoverData);
            }
        }


        FrameInfoPacket infoPacket = addFirstFramePacket(frame , datas.size());
        ArrayList<FrameDataPacket> dataPackets = new ArrayList<FrameDataPacket>();

        //Make the packet
        for(byte[] packetData : datas){
            dataPackets.add(addFrameDataPacket(packetData, frameID));
        }

        FramePackets framePackets = new FramePackets(infoPacket , dataPackets);

        mFramePackets.add(framePackets);*/
    }

    //Adds the first packet for a frame, this one will just have the frame's information
    private FrameInfoPacket addFirstFramePacket(AudioFrame frame, int numPackets){

        FrameInfoPacket frameInfoPacket = new FrameInfoPacket(frame, numPackets , mStreamID , mSongStartTime , mNextPacketID, frame.getLength());

        frameInfoPacket.putPacket(new DatagramPacket(frameInfoPacket.getData() , frameInfoPacket.getData().length));

        //Put the packet in the array
        synchronized (mPackets) {
            mPackets.add(mNextPacketID, frameInfoPacket);
        }
        mNextPacketID++;

        return frameInfoPacket;
    }

    //Takes in data and makes a packet out of it
    private FrameDataPacket addFrameDataPacket(byte[] frameData , int frameID){

        FrameDataPacket frameDataPacket = new FrameDataPacket(frameData , mStreamID , mNextPacketID, frameID);

        //Make the packet
        frameDataPacket.putPacket(new DatagramPacket(frameDataPacket.getData() , frameDataPacket.getData().length));

        //Put the packet in the array
        synchronized (mPackets) {
            mPackets.add(mNextPacketID, frameDataPacket);
        }
        mNextPacketID++;

        return frameDataPacket;
    }

    private FramePacket addFramePacket(AudioFrame frame){

        FramePacket framePacket = new FramePacket(frame , mStreamID ,mSongStartTime , mNextPacketID, frame.getLength());

        //Make the packet
        framePacket.putPacket(new DatagramPacket(framePacket.getData() , framePacket.getData().length));

        //Put the packet in the array
        synchronized (mPackets) {
            mPackets.add(mNextPacketID, framePacket);
        }
        mNextPacketID++;

        return framePacket;
    }

    private SongStartPacket createStartSongPacket(){

        SongStartPacket songStartPacket = new SongStartPacket(mSongStartTime , mStreamID , mNextPacketID, mSampleRate , mChannels , mMime , mDuration , mBitrate);

        songStartPacket.putPacket(new DatagramPacket(songStartPacket.getData() , songStartPacket.getData().length));

        synchronized (mPackets) {
            mPackets.add(songStartPacket);
        }
        mNextPacketID++;

        return songStartPacket;
    }

    public void lastPacket(){
        mLastPacketID = mNextPacketID;
        mLastFrameID = mFramePackets.size() - 1;
    }

    //This is called by ReaderBroadcasterBridge to push in the
    public void addFrames(ArrayList<AudioFrame> frames){
        synchronized(mPackets){
            for(AudioFrame frame : frames){
                handleFrame(frame);
            }
        }
    }

    public void setAudioTrackInfo(int sampleRate , int channels, String mime , long duration , int bitrate){
        mSampleRate = sampleRate;
        mChannels = channels;
        mMime = mime;
        mDuration = duration;
        mBitrate = bitrate;
        createStartSongPacket();
        synchronized (mPackets) {
            mPackets.add(new MimePacket(mMime, mNextPacketID, mStreamID));
        }
        mNextPacketID++;

    }

    public byte getStreamID(){
        return mStreamID;
    }
}