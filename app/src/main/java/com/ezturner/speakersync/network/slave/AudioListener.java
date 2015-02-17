package com.ezturner.speakersync.network.slave;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.master.AudioBroadcaster;
import com.ezturner.speakersync.network.ntp.SntpClient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;


/**
 * Created by Ethan on 2/8/2015.
 */
public class AudioListener {

    //TODO: Move Discovery code to DiscoveryHandler, and have that class handle both sides of Discovery

    //TODO: Move reliability code to ReliabilityHandler, and have that class handle both sides of Reliability, master and slave

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

    //The activity context
    private Context mContext;

    //The master that the listener is currently listening to
    private Master mMaster;

    //The broadcast address that will be used
    private InetAddress mAddress;

    private AudioTrackManager mAudioTrackManager;


    //The activity context
    private Context mContext;


    public AudioListener(Context context , AudioTrackManager atm){

        mAudioTrackManager = atm;
        mContext = context;

        mAddress = AudioBroadcaster.getBroadcastAddress();

    }

    //Start playing from a master, start listening to the stream
    public void playFromMaster(Master master , ArrayList<DatagramPacket> packets){
        mSntpClient = new SntpClient(master.getIP().toString());
        mPackets = packets;

    }

    //Starts the process of finding masters
    public void findMasters() {

    }

    private void broadcastMasters(){
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("master-discovered");
        // You can also include some extra data.

        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private Thread startListeningForPackets(){
        return new Thread(){
            public void run(){

            }
        };
    }


}
