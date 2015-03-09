package com.ezturner.speakersync.network.master;

import android.net.Network;
import android.util.Log;

import com.ezturner.speakersync.MyApplication;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.packets.MasterResponsePacket;

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
    private DatagramSocket mReceiveSocket;

    //DatagramSocket for the passive listeners out there holla out
    private DatagramSocket mSendSocket;

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
            mReceiveSocket = new DatagramSocket(CONSTANTS.DISCOVERY_MASTER_PORT);
            mReceiveSocket.setBroadcast(true);
            mSendSocket = new DatagramSocket(CONSTANTS.DISCOVERY_SLAVE_PORT);
            mSendSocket.setBroadcast(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mListenerThread = startPacketListener();

        mListenerThread.start();
    }


    private void sendStartPacket(){
        //Set packet type
        byte [] data = new byte[CONSTANTS.MASTER_START_PACKET];

        //Get port
        data = NetworkUtilities.combineArrays(data ,ByteBuffer.allocate(4).putInt(mParent.getPort()).array());

        //Get phone number
        byte[] number = MyApplication.getPhoneNumber().getBytes();

        //combine the two arrays
        data = NetworkUtilities.combineArrays(data, number);

        //Make the packet
        DatagramPacket outPacket = new DatagramPacket(data , data.length , NetworkUtilities.getBroadcastAddress() , CONSTANTS.DISCOVERY_SLAVE_PORT );

        //Send out packet
        try {
            mSendSocket.send(outPacket);
        } catch(IOException e){
            e.printStackTrace();
        }

    }

    private Thread startPacketListener(){
        return new Thread(){
            public void run(){
                //sendStartPacket();
                Log.d(LOG_TAG, "Packet Listener engaged , " + mParent.isStreamRunning());

                 while(mListening){
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
            mReceiveSocket.receive(packet);
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

        MasterResponsePacket resPacket = new MasterResponsePacket(mParent.getPort());

        byte[] data = resPacket.getData();

        //Make the packet
        DatagramPacket outPacket = new DatagramPacket(data , data.length , NetworkUtilities.getBroadcastAddress() , CONSTANTS.DISCOVERY_SLAVE_PORT);

        //Send out packet
        try {
            mSendSocket.send(outPacket);
        } catch(SocketException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        }
    }




    public synchronized void release(){
        mListening = false;
        mReceiveSocket.close();
        mSendSocket.close();
    }




}
