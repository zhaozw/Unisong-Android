package com.ezturner.speakersync.network.master;

import android.util.Log;

import com.ezturner.speakersync.MyApplication;
import com.ezturner.speakersync.MainActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

/**
 * Created by ezturner on 2/13/2015.
 */
public class MasterDiscoveryHandler {

    private final static String LOG_TAG = "MasterDiscoveryHandler";

    //DatagramSocket for the active listeners out there
    private DatagramSocket mSocket;

    //DatagramSocket for the passive listeners out there holla out
    private DatagramSocket mPassiveSocket;

    private Thread mListenerThread;
    
    //The AudioBroadcaster for when this is master side
    private AudioBroadcaster mParent;

    //Whether the master has been chosen and the class is now listening to it, or not.
    private boolean mListening;

    //The boolean that tells whether the slave has recieved a master and is awaiting others
    private boolean mIsDeciding;

    //The boolean used to tell if the timer that waits 75ms
    private boolean mHasRecievedDiscoverPacket;

    //The socket for listening for discovery packets
    private DatagramSocket mDiscoverySocket;



    public MasterDiscoveryHandler(AudioBroadcaster parent){

        mListening = true;
        mParent = parent;

        try {
            mSocket = new DatagramSocket(AudioBroadcaster.DISCOVERY_PORT  ,parent.getBroadcastAddress());
            mSocket.setBroadcast(true);
            mPassiveSocket = new DatagramSocket(AudioBroadcaster.DISCOVERY_PASSIVE_PORT , parent.getBroadcastAddress());
            mPassiveSocket.setBroadcast(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mListenerThread = startPacketListener();

        mListenerThread.start();
    }


    private void sendStartPacket(){
        //Get port
        byte[] data = ByteBuffer.allocate(4).putInt(mParent.getPort()).array();

        //Get phone number
        byte[] number = MainActivity.getPhoneNumber().getBytes();

        //combine the two arrays
        data = AudioBroadcaster.combineArrays(data, number);

        //Make the packet
        DatagramPacket outPacket = new DatagramPacket(data , data.length , mParent.getBroadcastAddress() , AudioBroadcaster.DISCOVERY_PORT + 1);

        //Send out packet
        try {
            mSocket.send(outPacket);
        } catch(IOException e){
            e.printStackTrace();
        }

        outPacket.setPort(AudioBroadcaster.DISCOVERY_PASSIVE_PORT);
        try {
            mPassiveSocket.send(outPacket);
        } catch(IOException e){
            e.printStackTrace();
        }

    }

    private Thread startPacketListener(){
        return new Thread(){
            public void run(){
                //sendStartPacket();
                Log.d(LOG_TAG, "Packet Listener engaged , " + mParent.isStreamRunning());
                while(mParent.isStreamRunning()){
                    Log.d(LOG_TAG , "Starting to listen");
                    listenForPacket();
                }
            }
        };
    }

    //Listens for packets
    private void listenForPacket(){
        byte[] data = new byte[1024];
        DatagramPacket packet = new DatagramPacket(data , data.length);

        Log.d(LOG_TAG , "Starting to listen");
        try {
            mSocket.receive(packet);
        } catch(IOException e){
            e.printStackTrace();
            return;
        }


        handlePacket(packet);


    }

    //Takes in a packet, and sends back the port in use and
    private void handlePacket(DatagramPacket packet){
        InetAddress addr = packet.getAddress();

        Log.d(LOG_TAG , "Packet received , from : " + addr.toString());

        //Get port
        byte[] data = ByteBuffer.allocate(4).putInt(mParent.getPort()).array();

        //Get phone number
        byte[] number = MyApplication.getPhoneNumber().getBytes();

        //combine the two arrays
        data = AudioBroadcaster.combineArrays(data, number);

        //Make the packet
        DatagramPacket outPacket = new DatagramPacket(data , data.length , mParent.getBroadcastAddress() , AudioBroadcaster.DISCOVERY_PORT);

        //Send out packet
        try {
            mSocket.send(outPacket);
        } catch(SocketException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        }
    }


}
