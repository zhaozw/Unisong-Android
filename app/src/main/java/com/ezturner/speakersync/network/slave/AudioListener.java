package com.ezturner.speakersync.network.slave;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.master.AudioBroadcaster;
import com.ezturner.speakersync.network.ntp.SntpClient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;


/**
 * Created by Ethan on 2/8/2015.
 * This class handles listening to and audio stream. Its component discovery handler handles
 * discovery, and the reliability handler handles reliability
 *
 */
public class AudioListener {


    //The boolean indicating whether the above objects are in use(a master has not been chosen yet)
    private boolean mIsDeciding;

    //The Sntp client for time synchronization
    private SntpClient mSntpClient;

    //The time offset between this client and the master
    private long mTimeOffset;

    //The multicast socket for discovery and control
    private MulticastSocket mManagementSocket;

    //the socket for receiving the stream
    private DatagramSocket mSocket;

    //The ArrayList of received packets
    private ArrayList<DatagramPacket> mPackets;

    //The master that the listener is currently listening to
    private Master mMaster;

    //The broadcast address that will be used
    private InetAddress mAddress;

    //The port that the stream will be on
    private int mPort;

    private AudioTrackManager mAudioTrackManager;

    //The discovery handler, which will handle finding and choosing the
    private SlaveDiscoveryHandler mSlaveDiscoveryHandler;

    //The Slave reliability handler which handles packet reliability
    private SlaveReliabilityHandler mSlaveReliabilityHandler;

    //The activity context
    private Context mContext;



    public AudioListener(Context context , AudioTrackManager atm){

        Log.d("ezturner" , "Audio Listener Started");
        mAudioTrackManager = atm;
        mContext = context;

        mSlaveDiscoveryHandler = new SlaveDiscoveryHandler(this , mContext);

        mAddress = NetworkUtilities.getBroadcastAddress();

    }

    //Start playing from a master, start listening to the stream
    public void playFromMaster(Master master , ArrayList<DatagramPacket> packets, SntpClient client){
        mSntpClient = client;
        mPackets = packets;
        mMaster = master;

        mPort = master.getPort();

        mSlaveReliabilityHandler = new SlaveReliabilityHandler(master.getIP());
    }

    //Starts the process of finding masters
    public void findMasters() {
        mSlaveDiscoveryHandler.findMasters();
    }

    private Thread startListeningForPackets(){
        return new Thread(){
            public void run(){

            }
        };
    }

    public long getNextFrameWriteTime(){
        return 0;
    }


}
