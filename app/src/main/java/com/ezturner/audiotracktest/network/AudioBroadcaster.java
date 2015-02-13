package com.ezturner.audiotracktest.network;

import android.util.Log;

import com.ezturner.audiotracktest.audio.AudioFrame;
import com.ezturner.audiotracktest.network.ntp.NtpServer;

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
    public static final int PORT = 1731;
    //public static final String CONTROL_MUTLICAST_IP ="238.17.0.29";

    private final byte STREAM_PACKET_ID = 0;
    private final byte SONG_START_PACKET_ID = 1;


    public int MAX_PACKET_SIZE = 2048;

    //The IP that the broadcast stream will be sent on
    private InetAddress mBroadcastIP;

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
    private ReliabilityHandler mReliabilityHandlder;

    //Stream ID, so that we can tell when we get packets from an old stream
    private byte streamID;



    public AudioBroadcaster(){
        try {
            //Disabled because android does not support multicast uniformly
            //Start the socket for giving out the multicast address
            //mControlSocket = new MulticastSocket(PORT);
            //mControlSocket.joinGroup(Inet4Address.getByName(CONTROL_MUTLICAST_IP));

            //Randomize the IP that the stream will be multicast on
            //streamIP = randomizeStreamIP();

            mBroadcastIP = getBroadcastAddress();

            //Start the socket for the actual multicast stream
            mStreamSocket = new DatagramSocket(PORT);

            //Start the NTP server for syncing the playback
            NtpServer.startNtpServer();

        } catch(IOException e){
            e.printStackTrace();
        }

        streamID = 0;
        try {
            //streamIP = randomizeStreamIP();
            mBroadcastIP = getBroadcastAddress();
        } catch (Exception e){
            e.printStackTrace();
        }

        packets = new TreeMap<Integer,DatagramPacket>();


    }

    //Starts streaming the song, starts the reliability listeners, and starts the control listener
    public void startSongStream(){
        //If another stream is running,
        if(mStreamRunning){
            //fix this code so it works
            //mReliabilityListener.stop();
        }
        streamID++;

        packets = new TreeMap<Integer, DatagramPacket>();

    }

    //Returns the IP address of the local interface. From online.
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

    /*
    private InetAddress randomizeStreamIP() throws UnknownHostException{
        return Inet4Address.getByName(238 + "." + random.nextInt(255) + "." + random.nextInt(255) + "." + random.nextInt(255));
    }*/

    public static InetAddress getBroadcastAddress() throws SocketException , UnknownHostException{
        System.setProperty("java.net.preferIPv4Stack", "true");
        for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
            NetworkInterface ni = niEnum.nextElement();
            if (!ni.isLoopback()) {
                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                    return Inet4Address.getByName(interfaceAddress.getBroadcast().toString().substring(1));
                }
            }
        }
        return null;df
    }


    //Broadcasts a streaming packet
    private void broadcastStreamPacket(int packetID){
        try {
            mStreamSocket.send(packets.get(packetID));
        } catch(IOException e){
            e.printStackTrace();
        }
    }


    public void addPacket(AudioFrame frame){

        //turn packet type into a byte array for combination , and put the stream ID in there
        byte[] packetType = new byte[]{STREAM_PACKET_ID , streamID};

        //Convert the packet ID to byte for transmission.
        //TODO: Decide :Should this just be a two-byte value?
        byte[] packetIDByte = ByteBuffer.allocate(4).putInt(frame.getID()).array();

        //Get the data for the time to play
        byte[] playTime = ByteBuffer.allocate(4).putLong(System.currentTimeMillis() + 250).array();

        //Get the data for the MP3 frame
        byte[] data = frame.getData();


        //Combines the various byte arrays into
        packetType = combineArrays(packetType , packetIDByte);
        packetType = combineArrays(packetType , playTime);


        data = combineArrays(packetType , packetIDByte);

        //Make the packet
        DatagramPacket packet = new DatagramPacket(data, data.length, mBroadcastIP , PORT);

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

    //Combines two arrays into one, from stackOverflow
    static byte[] combineArrays(byte[] a, byte[] b){
        int aLen = a.length;
        int bLen = b.length;
        byte[] c= new byte[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }
}
