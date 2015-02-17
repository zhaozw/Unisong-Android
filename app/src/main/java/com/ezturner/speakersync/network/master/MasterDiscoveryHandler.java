package com.ezturner.speakersync.network.master;

import android.content.Context;
import android.util.Log;

import com.ezturner.speakersync.MainActivity;
import com.ezturner.speakersync.network.slave.AudioListener;
import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.ntp.SntpClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by ezturner on 2/13/2015.
 */
public class MasterDiscoveryHandler {


    private DatagramSocket mSocket;

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

    //The thread for active discovery for the save
    private Thread mActiveDiscoveryThread;

    public MasterDiscoveryHandler(AudioBroadcaster parent){

        mListening = true;
        mParent = parent;

        try {
            mSocket = new DatagramSocket(AudioBroadcaster.DISCOVERY_PORT , AudioBroadcaster.getBroadcastAddress());
            mSocket.setBroadcast(true);
        } catch (Exception e) {
            Log.e("ezturner", e.toString());
        }

        mListenerThread = startPacketListener();

        mListenerThread.start();
    }



    private Thread startPacketListener(){
        return new Thread(){
            public void run(){
                while(mBroadcasterParent.isStreamRunning()){
                    listenForPacket();
                }
            }
        };
    }

    //Listens for packets
    private void listenForPacket(){
        byte[] data = new byte[512];
        DatagramPacket packet = new DatagramPacket(data , data.length);

        try {
            mSocket.receive(packet);
        } catch(IOException e){
            Log.e("ezturner" , e.toString());
            return;
        }
        handlePacket(packet);


    }



}
