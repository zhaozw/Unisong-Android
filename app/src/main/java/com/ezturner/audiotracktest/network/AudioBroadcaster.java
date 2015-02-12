package com.ezturner.audiotracktest.network;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
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

    public final int PORT = 1731;

    private final byte STREAM_PACKET_ID = 0;
    private final byte SONG_CHANGE_PACKET_ID = 1;


    public int MAX_PACKET_SIZE = 2048;

    //The IP that the multicast stream will be sent on
    public InetAddress broadcastIP;

    //The multicast listener for giving out the IP of the multicast stream
    private MulticastSocket mManagementSocket;

    //The listener for when a client requests a packet to be re-sent
    private DatagramSocket mReliabilitySocket;

    //The thread that will listen for reliability packets
    private Thread mReliabilityListener;

    //True if the listeners are running, false otherwise
    private boolean mStreamRunning;

    //Random object, used to randomize multicast stream IP
    static private Random random = new Random();

    //Map of packet IDs and their audio data
    private Map<Integer, byte[]> packets;

    //Stream ID, so that we can tell when we get packets from an old stream
    private byte streamID;

    public AudioBroadcaster(){
        try {
            mManagementSocket = new MulticastSocket(PORT);
            mManagementSocket.joinGroup(Inet4Address.getByName("238.17.0.29"));

            mReliabilitySocket = new DatagramSocket(PORT);

        } catch(IOException e){
            e.printStackTrace();
        }

        streamID = 0;
        try {
            broadcastIP = randomizeBroadcastIP();
        } catch (UnknownHostException e){
            e.printStackTrace();
        }

        packets = new TreeMap<Integer, byte[]>();


    }

    public void startSongStream(){
        //If another stream is running,
        if(mStreamRunning){
            //fix this code so it works
            //mReliabilityListener.stop();
        }

        mReliabilityListener = startListener();
        mReliabilityListener.start();

        streamID++;

        packets = new TreeMap<Integer, byte[]>();
    }

    private Thread startListener(){
        return new Thread(){
            public void run(){
                while(mStreamRunning){
                    recieveReliabilityPacket();
                }
            }
        });
    }

    public void setListenerState(boolean isRunning){
        if(isRunning){
            mStreamRunning = false;
        } else {
            mStreamRunning = true;
            mReliabilityListener.start();
        }
    }

    private void recieveReliabilityPacket(){
        byte[] data = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(data , 2048);

        try {
            mReliabilitySocket.receive(packet);
            handleReliabilityPacket(packet);
        } catch(IOException e){
            e.printStackTrace();
        }

        handleReliabilityPacket(packet);
    }

    private void handleReliabilityPacket(DatagramPacket packet){
        //Get data
        byte[] data = packet.getData();

        //Feed the data into a Bytebuffer
        ByteBuffer wrapped = ByteBuffer.wrap(data);

        //get stream ID and check it against current
        byte requestedStreamID = wrapped.get();

        if(requestedStreamID != streamID){
            return;
        }

        //Convert data to an int
        int packetID = wrapped.getInt();

        broadcastStreamPacket(packetID);
    }

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

    private InetAddress randomizeBroadcastIP() throws UnknownHostException{
        return Inet4Address.getByName(238 + "." + random.nextInt(255) + "." + random.nextInt(255) + "." + random.nextInt(255));
    }


    //Broadcasts a streaming packet
    private void broadcastStreamPacket(int packetID){

        //turn packet type into a byte array for combination
        byte[] packetType = new byte[]{STREAM_PACKET_ID};

        //Convert the packet ID to byte for transmission
        byte[] packetIDByte = ByteBuffer.allocate(4).putInt(packetID).array();

        //Get the data for the time to play
        byte[] playTime = ByteBuffer.allocate(4).putLong(System.currentTimeMillis() + 250).array();

        //Get the data for the MP3 frame
        byte[] data = packets.get((Integer) packetID);


        packetType = combineArrays(packetType , packetIDByte);
        packetType = combineArrays(packetType , playTime);

        data = combineArrays(packetType , packetIDByte);

        DatagramPacket packet = new DatagramPacket(data, data.length, broadcastIP , PORT);

        try {
            mReliabilitySocket.send(packet);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private void broadcastStartStreamPacket(){

    }

    static byte[] combineArrays(byte[] a, byte[] b){
        int aLen = a.length;
        int bLen = b.length;
        byte[] c= new byte[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }
}
