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
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

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

    //Map of packet IDs and their audio data
    private Map<Integer, NetworkPacket> mPackets;

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

    private int mInterval = 5000; // 5 seconds by default, can be changed later
    private Handler mHandler;

    //The ID of the packet to be sent next
    private int mNextPacketSendID;

    //The ID of the next packet to be made
    private int mNextPacketID;

    //The ID of the last packet in this stream
    private int mLastPacketID;

    //The AudioTrackManager that handles the playback of the audio data on this device
    private AudioTrackManager mManager;

    //The object that handles the reading and decoding of all of dem music
    private AudioFileReader mReader;

    //The boolean that lets us know if we are still broadcasting
    private boolean mIsBroadcasting;

    //The NTP server for time synchronization
    private NtpServer mNtpServer;



    //Makes an AudioBroadcaster object
    //Creates the sockets, starts the NTP server and instantiates variables
    public AudioBroadcaster(AudioTrackManager manager , AudioFileReader reader){

        //TODO: When not testing, get rid of this comment
        mPort = CONSTANTS.STREAM_PORT_BASE;// + random.nextInt(PORT_RANGE);
        //TODO: Listen for other streams and ensure that you don't use the same port
        try {
            mStreamIP = NetworkUtilities.getBroadcastAddress();
            //Start the socket for the actual stream
            mStreamSocket = new DatagramSocket(getPort());

            mDiscoveryHandler = new MasterDiscoveryHandler(this);

            //Start the NTP server for syncing the playback
            //TODO: Fix
            mNtpServer = new NtpServer();


        } catch(IOException e){
            e.printStackTrace();
        }

        //set the stream ID to zero
        mStreamID = 0;

        //Make the map of the packets
        mPackets = new HashMap<Integer , NetworkPacket>();

        //Makes the handler for broadcasting packets
        mHandler = new Handler();

        //Set the next packet to be created to 0
        mNextPacketID = 0;

        //Set the next packet to be created to be 0
        mNextPacketSendID = 0;

        mIsBroadcasting = false;

        mManager = manager;

        mReader = reader;

        //TODO: Switch to false, just for the test
        mStreamRunning = true;

        mLastPacketID = -1;

        mHandler.postDelayed(mSongStreamStart, 5000);
        //TODO: Delete this when the UI is integrated! this is just for test
        //startSongStream();

        //TODO: Delete this too
        try {
            mReader.readFile(MediaService.TEST_FILE_PATH);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //TODO: put in some code that will ensure that we haven't gotten off track due to rounding errors or  something like that

    Runnable mPacketSender = new Runnable() {
        @Override
        public void run() {
            if(mIsBroadcasting) {
                NetworkPacket packet = mPackets.get(mNextPacketSendID);

                //Figure out what to do with the next packet
                /*
                if(packet instanceof SongStartPacket){
                    //If the song is just starting, then broadcast the whole next frame and wait for the next frame to need to be sent
                    Log.d(LOG_TAG, "SongStartPacket found");
                    broadcastUntilNextFrame();
                } else if(packet instanceof FrameDataPacket ){
                    Log.d(LOG_TAG, "FrameDataPacket found");
                    broadcastUntilNextFrame();
                } else if(packet instanceof  FrameInfoPacket){
                    Log.d(LOG_TAG, "SongStartPacket found");
                    broadcastUntilNextFrame();
                }*/

                broadcastUntilNextFrame();



                if(mNextPacketSendID != mLastPacketID) {
                    Log.d(LOG_TAG , "mNextPacketSendID: " + mNextPacketSendID);
                    FrameInfoPacket tempPacket = (FrameInfoPacket) mPackets.get(mNextPacketSendID);
                    AudioFrame frame = tempPacket.getFrame();
                    mInterval =(int) frame.getLength() / 1000;
                    mHandler.postDelayed(mPacketSender, mInterval);
                }
            }
        }
    };

    private void broadcastUntilNextFrame(){
        Log.d(LOG_TAG , "Broadcasting Packet #" + mNextPacketSendID);
        broadcastStreamPacket(mNextPacketSendID);
        mNextPacketSendID++;
        //And keep broadcasting the frame until we hit the next one
        while( !(mPackets.get(mNextPacketSendID) instanceof FrameInfoPacket && mNextPacketSendID != mLastPacketID) ){
            broadcastStreamPacket(mNextPacketSendID);
            mNextPacketSendID++;
        }
    }



    Runnable mSongStreamStart = new Runnable() {
        @Override
        public void run() {
            Log.d(LOG_TAG , "Stream starting!");
            Log.d(LOG_TAG , "The number of packets is : " + mPackets.size());
            startSongStream();
        }
    };

    void startRepeatingTask() {
        mPacketSender.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mPacketSender);
    }

    //Starts streaming the song, starts the reliability listeners, and starts the control listener
    public void startSongStream(){
        //If another stream is running,
        if(mStreamRunning){
            //TODO: fix this code so it works
            //mReliabilityListener.stop();
        }

        //The start time in milliseconds
        mSongStartTime = System.currentTimeMillis() + 300;
        mNextFrameTime = mSongStartTime;

        if(mStreamID == 240){
            mStreamID = 0;
        } else {
            mStreamID++;
        }

        mPackets = new HashMap<Integer, NetworkPacket>();
        try {
            mReader.readFile(MediaService.TEST_FILE_PATH);
        } catch(IOException e){
            e.printStackTrace();
        }

        createStartSongPacket();

        mIsBroadcasting = true;
        mHandler.postDelayed(mPacketSender , 25);
    }

    //Broadcasts a streaming packet
    private void broadcastStreamPacket(int packetID){
        synchronized(mPackets){
            //TODO : doooooooooo
        }
        if() {
            try {
                Log.d(LOG_TAG , "Packet to be sent is: " + packetID);
                mStreamSocket.send(mPackets.get(packetID).getPacket());
            } catch (IOException e) {
                //TODO: if this failes, retry
                e.printStackTrace();
            }
        } else {
            try {
                Thread.sleep(20);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
            if(mPackets.containsKey(packetID)){
                try {
                    mStreamSocket.send(mPackets.get(packetID).getPacket());
                } catch (IOException e) {
                    //TODO: if this failes, retry
                    e.printStackTrace();
                }
            }

        }
    }

    public void rebroadcastPacket(int packetID){
        if(mPackets.containsKey(packetID)) {
            broadcastStreamPacket(packetID);
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

        addFirstFramePacket(frame ,datas.size());

        for(byte[] packetData : datas){
            addFrameDataPacket(packetData , frameID);
        }

    }

    //Adds the first packet for a frame, this one will just have the frame's information
    private synchronized void addFirstFramePacket(AudioFrame frame, int numPackets){

        FrameInfoPacket frameInfoPacket = new FrameInfoPacket(frame, numPackets , mStreamID , mSongStartTime , mNextPacketID, frame.getLength());

        frameInfoPacket.putPacket(new DatagramPacket(frameInfoPacket.getData() , frameInfoPacket.getData().length , mStreamIP , getPort()));

        //Put the packet in the array
        mPackets.put(mNextPacketID , frameInfoPacket);
        mNextPacketID++;
    }

    //Takes in data and makes a packet out of it
    private synchronized void addFrameDataPacket(byte[] frameData , int frameID){

        FrameDataPacket frameDataPacket = new FrameDataPacket(frameData , mStreamID , mNextPacketID, frameID);

        byte[] data = frameDataPacket.getData();

        //Make the packet
        DatagramPacket packet = new DatagramPacket(data, data.length, mStreamIP , getPort());

        //Put the packet in the array
        mPackets.put(mNextPacketID , frameDataPacket);
        mNextPacketID++;
    }

    private synchronized void createStartSongPacket(){

        SongStartPacket songStartPacket = new SongStartPacket(mSongStartTime , mStreamID , mNextPacketID);

        byte[] data = songStartPacket.getData();

        DatagramPacket packet = new DatagramPacket(data, data.length, mStreamIP , getPort());
        mPackets.put(mNextPacketID , songStartPacket);
        mNextPacketID++;
    }
    
    public void lastPacket(){
        mLastPacketID = mNextPacketID;
    }

    //This is called by ReaderBroadcasterBridge to push in the
    public void pushFrames(ArrayList<AudioFrame> frames){

    }
}