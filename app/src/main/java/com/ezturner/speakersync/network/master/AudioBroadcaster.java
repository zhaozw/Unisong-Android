package com.ezturner.speakersync.network.master;

import android.util.Log;
import android.os.Handler;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.ntp.NtpServer;

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
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Created by Ethan on 2/8/2015.
 */
public class AudioBroadcaster {


    //TODO: Find a good place to put this and the control multicast IP
    public static final int STREAM_PORT_BASE = 55989;
    public static final int PORT_RANGE = 800;
    public static final int DISCOVERY_PORT = 55988;
    public static final int DISCOVERY_PASSIVE_PORT = 55987;
    public static final String CONTROL_MUTLICAST_IP ="238.17.0.29";

    private final byte STREAM_PACKET_ID = 0;
    private final byte SONG_START_PACKET_ID = 1;

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
    private Map<Integer, DatagramPacket> packets;

    //The object that handles all reliability stuff
    private MasterReliabilityHandler mReliabilityHandlder;

    //Stream ID, so that we can tell when we get packets from an old stream
    private byte streamID;

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
    private int mNextPacketId;

    //Makes an AudioBroadcaster object
    //Creates the sockets, starts the NTP server and instantiates variables
    public AudioBroadcaster(){

        mPort = STREAM_PORT_BASE;// + random.nextInt(PORT_RANGE);
        //TODO: Listen for other streams and ensure that you don't use the same port
        try {
            mStreamIP = getBroadcastAddress();
            //Start the socket for the actual stream
            mStreamSocket = new DatagramSocket(getPort() , mStreamIP);

            mDiscoveryHandler = new MasterDiscoveryHandler(this);


            //Start the NTP server for syncing the playback
            NtpServer.startNtpServer();


        } catch(IOException e){
            e.printStackTrace();
        }

        //set the stream ID to zero
        streamID = 0;

        //Make the map of the packets
        packets = new TreeMap<Integer , DatagramPacket>();

        //Makes the handler for broadcasting packets
        mHandler = new Handler();

        //Set the next packet to be sent to 1
        mNextPacketId = 1;
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

        if(streamID == 240){
            streamID = 0;
        } else {
            streamID++;
        }

        packets = new TreeMap<Integer, DatagramPacket>();

    }

    private void sendFirstPackets(){

    }

    //Returns the IP address of the local interface. Code is from online.
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("ezturner", ex.toString());
        }
        return null;
    }


    //Randomizes a multicast stream IP
    private InetAddress randomizeStreamIP() throws UnknownHostException{
        return Inet4Address.getByName(238 + "." + random.nextInt(255) + "." + random.nextInt(255) + "." + random.nextInt(255));
    }


    //Returns the broadcast IP address for the current network
    //TODO: Implement exception handling
    public static InetAddress getBroadcastAddress(){
        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements(); ) {
                NetworkInterface ni = niEnum.nextElement();
                if (!ni.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                        if (interfaceAddress.getBroadcast() != null) {
                            return Inet4Address.getByName(interfaceAddress.getBroadcast().toString().substring(1));
                        }
                    }
                }
            }
        } catch (SocketException e){
            Log.e("ezturner" , e.toString());
        } catch(UnknownHostException e){
            Log.e("ezturner", e.toString());
        }
        return null;
    }

    //Broadcasts a streaming packet
    private void broadcastStreamPacket(int packetID){
        try {
            mStreamSocket.send(packets.get(packetID));
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //Takes in an AudioFrame and makes a packet out of it
    public void addPacket(AudioFrame frame){

        //turn packet type into a byte array for combination , and put the stream ID in there
        byte[] packetType = new byte[]{STREAM_PACKET_ID , streamID};

        //Convert the packet ID to byte for transmission.
        //TODO: Decide :Should this just be a two-byte value?
        byte[] packetIDByte = ByteBuffer.allocate(4).putInt(frame.getID()).array();

        //Get the data for the time to play
        byte[] playTime = ByteBuffer.allocate(4).putLong(frame.getPlayTime()).array();

        //Get the data for the MP3 frame
        byte[] data = frame.getData();

        //Combines the various byte arrays into
        packetType = combineArrays(packetType , packetIDByte);
        packetType = combineArrays(packetType , playTime);

        data = combineArrays(packetType , packetIDByte);

        //Make the packet
        DatagramPacket packet = new DatagramPacket(data, data.length, mStreamIP , STREAM_PORT_BASE);

        //Put the packet in the array
        packets.put(frame.getID() , packet);
    }

    public void rebroadcastPacket(int packetID){
        broadcastStreamPacket(packetID);
    }

    public boolean isStreamRunning(){
        return mStreamRunning;
    }

    public int getCurrentStreamID(){
        return streamID;
    }

    public int getPort(){
        return mPort;
    }

    //Combines two arrays into one, from stackOverflow
    public static byte[] combineArrays(byte[] a, byte[] b){
        int aLen = a.length;
        int bLen = b.length;
        byte[] c= new byte[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
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
}