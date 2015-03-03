package com.ezturner.speakersync.network.master;

import android.util.Log;
import android.os.Handler;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.ntp.NtpServer;
import com.ezturner.speakersync.network.CONSTANTS;

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
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Created by Ethan on 2/8/2015.
 */
public class AudioBroadcaster {


    public static final String CONTROL_MUTLICAST_IP ="238.17.0.29";

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
    private Map<Integer, DatagramPacket> mPackets;

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
    private int mNextPacketSendId;

    //The ID of the next packet to be made
    private int mNextPacketId;

    //The AudioTrackManager that handles the playback of the audio data on this device
    private AudioTrackManager mManager;

    //The leftover bytes from a previous AudioFrame, since the packets need to be 1024 bytes
    private byte[] mLeftoverBytes;

    //Makes an AudioBroadcaster object
    //Creates the sockets, starts the NTP server and instantiates variables
    public AudioBroadcaster(AudioTrackManager manager){

        mPort = CONSTANTS.STREAM_PORT_BASE;// + random.nextInt(PORT_RANGE);
        //TODO: Listen for other streams and ensure that you don't use the same port
        try {
            mStreamIP = NetworkUtilities.getBroadcastAddress();
            //Start the socket for the actual stream
            mStreamSocket = new DatagramSocket(getPort() , mStreamIP);

            mDiscoveryHandler = new MasterDiscoveryHandler(this);

            //Start the NTP server for syncing the playback
            NtpServer.startNtpServer();


        } catch(IOException e){
            e.printStackTrace();
        }

        //set the stream ID to zero
        mStreamID = 0;

        //Make the map of the packets
        mPackets = new TreeMap<Integer , DatagramPacket>();

        //Makes the handler for broadcasting packets
        mHandler = new Handler();

        //Set the next packet to be created to 0
        mNextPacketId = 0;

        //Set the next packet to be created to be 0
        mNextPacketSendId = 0;

        mLeftoverBytes = new byte[0];
        mManager = manager;

        mStreamRunning = false;
    }


    Runnable mPacketSender = new Runnable() {
        @Override
        public void run() {
            broadcastStreamPacket(mNextPacketId); //this function can change value of mInterval.
            mHandler.postDelayed(mPacketSender, mInterval);
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

        mSongStartTime = System.currentTimeMillis() * 1000 + 250000;
        mNextFrameTime = mSongStartTime;

        if(mStreamID == 240){
            mStreamID = 0;
        } else {
            mStreamID++;
        }

        mPackets = new TreeMap<Integer, DatagramPacket>();

    }

    private void sendFirstPackets(){

    }

    //Broadcasts a streaming packet
    private void broadcastStreamPacket(int packetID){
        try {
            mStreamSocket.send(mPackets.get(packetID));
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void rebroadcastPacket(int packetID){
        if(mPackets.containsKey(packetID)) {
            broadcastStreamPacket(packetID);
        }
    }

    public boolean isStreamRunning(){
        //TODO: Fix this, this is a test fix
        return true;
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

        ArrayList<byte[]> datas = new ArrayList<byte[]>();


            //Combine the data with the leftover bytes if mLeftoverBytes has data
            if(mLeftoverBytes.length != 0){
                data = NetworkUtilities.combineArrays(mLeftoverBytes , data);
            }

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
                dataPos += 1024;
                bytesLeft -= 1024;

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
            addFrameDataPacket(packetData);
        }

    }

    //Adds the first packet for a frame, this one will just have the frame's information
    private void addFirstFramePacket(AudioFrame frame, int numPackets){

        //turn packet type into a byte array for combination , and put the stream ID in there
        byte[] packetType = new byte[]{CONSTANTS.FRAME_INFO_PACKET_ID , mStreamID};

        byte[] packetIdByte = ByteBuffer.allocate(4).putInt(mNextPacketId).array();

        //Throw the frame Id in a byte array, and throw all the other data in byte
        // arrays and combine them into the data that will go in the packet
        byte[] frameId = ByteBuffer.allocate(4).putInt(frame.getID()).array();

        byte[] playTime = ByteBuffer.allocate(8).putLong(frame.getPlayTime()).array();

        byte[] numPacketsArr = ByteBuffer.allocate(4).putInt(numPackets).array();

        byte[] data = NetworkUtilities.combineArrays(packetType , packetIdByte);

        data = NetworkUtilities.combineArrays(data , frameId);

        data = NetworkUtilities.combineArrays(data , playTime);

        data = NetworkUtilities.combineArrays(data , numPacketsArr);

        //Make the packet
        DatagramPacket packet = new DatagramPacket(data, data.length, mStreamIP , getPort());

        //Put the packet in the array
        mPackets.put(mNextPacketId , packet);
        mNextPacketId++;
    }

    //Takes in data and makes a packet out of it
    private void addFrameDataPacket(byte[] data){

        //turn packet type into a byte array for combination , and put the stream ID in there
        byte[] packetType = new byte[]{CONSTANTS.FRAME_DATA_PACKET_ID , mStreamID};

        //Convert the packet ID to byte for transmission.
        //TODO: Decide :Should this just be a two-byte value?
        byte[] packetIDByte = ByteBuffer.allocate(4).putInt(mNextPacketId).array();

        //Combines the various byte arrays into
        packetType = NetworkUtilities.combineArrays(packetType , packetIDByte);

        data = NetworkUtilities.combineArrays(packetType , packetIDByte);

        //Make the packet
        DatagramPacket packet = new DatagramPacket(data, data.length, mStreamIP , getPort());

        //Put the packet in the array
        mPackets.put(mNextPacketId , packet);
        mNextPacketId++;
    }
    
}