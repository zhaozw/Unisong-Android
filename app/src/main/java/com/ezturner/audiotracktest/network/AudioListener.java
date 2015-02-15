package com.ezturner.audiotracktest.network;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ezturner.audiotracktest.MediaService;
import com.ezturner.audiotracktest.audio.AudioTrackManager;
import com.ezturner.audiotracktest.network.ntp.SntpClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by Ethan on 2/8/2015.
 */
public class AudioListener {

    //The ArrayLists for when there are two clients and you want to buffer for both of them
    private ArrayList<SntpClient> mTempClients;
    private ArrayList<DatagramSocket> mTempSockets;
    private ArrayList<ArrayList<DatagramPacket>> mTempPacketStorage;
    private ArrayList<Master> mTempMasters;

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

    //The socket used when a user tries to manually join a stream or starts up the device.
    private DatagramSocket mDiscoverySocket;
    private DatagramSocket mPassiveDiscoverySocket;

    //The listener for when a client requests a packet to be re-sent
    private Socket mReliabilitySocket;

    private Thread mActiveDiscoveryThread;
    private Thread mPassiveDiscoveryThread;

    //The boolean used to tell if the timer that waits 75ms
    private boolean mHasRecievedDiscoverPacket;

    //The ArrayList of received packets
    private ArrayList<DatagramPacket> mPackets;

    //The activity context
    private Context mContext;

    //The master that the listener is currently listening to
    private Master mMaster;

    //Whether the listener is passively listening for declarations
    private boolean mPassiveListen;

    //Whether the master has been chosen and the class is now listening to it, or not.
    private boolean mListening;

    private AudioTrackManager mAudioTrackManager;

    public AudioListener(Context context , AudioTrackManager atm){

        mAudioTrackManager = atm;
        mContext = context;

        try {
            if(MediaService.isMulticast()) {

                mManagementSocket = new MulticastSocket(AudioBroadcaster.DISCOVERY_PORT);
                mManagementSocket.joinGroup(Inet4Address.getByName("238.17.0.29"));
            } else {
                mDiscoverySocket = new DatagramSocket(AudioBroadcaster.DISCOVERY_PORT , AudioBroadcaster.getBroadcastAddress());
                mPassiveDiscoverySocket = new DatagramSocket(AudioBroadcaster.DISCOVERY_PORT , AudioBroadcaster.getBroadcastAddress());
            }
        } catch(IOException e){
            e.printStackTrace();
        }

        mActiveDiscoveryThread = getActiveDiscoveryThread();
        //mActiveDiscoveryThread.start();

        mPassiveDiscoveryThread = getPassiveDiscoveryThread();
        mPassiveDiscoveryThread.start();

        mHasRecievedDiscoverPacket = false;

        mTempClients = new ArrayList<SntpClient>();

        mTempSockets = new ArrayList<DatagramSocket>();

        mIsDeciding = false;

        mPassiveListen = true;

        mListening = false;
    }

    //Start playing from a master, start listening to the stream
    public void playFromMaster(Master master){
        mSntpClient = new SntpClient(master.getIP().toString());
        mPackets = new ArrayList<DatagramPacket>();
    }

    //Sends a request
    private void sendDiscoveryRequest(){

        DatagramPacket packet = new DatagramPacket(new byte[64] , 64);

        try {
            mDiscoverySocket.send(packet);
        } catch(IOException e){
            Log.e("ezturner", e.toString());
        }
        listenForDiscoveryResponse(true);
    }


    //Start listening to the multicast stream
    private void startListeningMulticast(){
        if(! MediaService.multicastLockIsHeld()) {
            MediaService.aquireMulticastLock();
        }
    }


    //Stop listening to the multicast stream
    private void stopListeningMulticast(){
        if(MediaService.multicastLockIsHeld()) {
            MediaService.releaseMulticastLock();
        }

    }



    private Thread getActiveDiscoveryThread(){
        return new Thread(){
            public void run(){
                mPassiveListen = false;
                mPassiveDiscoverySocket.close();
                mIsDeciding = true;

                while(mIsDeciding) {
                    sendDiscoveryRequest();
                }
            }
        };
    }

    private Thread getPassiveDiscoveryThread(){
        return new Thread(){
            public void run(){
                while(mPassiveListen) {
                    listenForDiscoveryResponse(false);
                }
            }
        };
    }

    //Listen for a discovery packet, and if you get one start listening and modify the UI to ask the user
    //if they want to play the stream
    private synchronized void listenForDiscoveryResponse(boolean active){

        DatagramPacket packet = new DatagramPacket(new byte[256] , 256);
        if(active) {
            try {
                mDiscoverySocket.setSoTimeout(5000);
            } catch (SocketException e) {
                Log.e("ezturner", e.toString());
            }
        }

        try {
            if(active) {
                mDiscoverySocket.receive(packet);
            } else {
                mPassiveDiscoverySocket.receive(packet);
            }
        } catch(IOException e){
            Log.e("ezturner" , e.toString());
        }



        byte[] data = packet.getData();

        InetAddress addr = packet.getAddress();

        byte[] portArr = new byte[]{data[0 ] , data[1] , data[2] , data[3]};

        ByteBuffer wrapped = ByteBuffer.wrap(portArr); // big-endian by default
        int port = wrapped.getInt(); // 1
        //TODO: Get phone number from this. Needs to be tested with a real phone to see what the format of the phone number
        //TODO: string is and how long it is, and more importantly how many bytes it takes up

        Master master = new Master(port ,"4252414577" , addr);

        if(!active){

        } else {

            //Checks to see if we've already recieved this master so we don't get duplicates
            if (mTempMasters.contains(master)) {
                return;
            }

            //Creates an SntpClient

            mTempClients.add(new SntpClient(addr.toString()));
            try {


                DatagramSocket socket = new DatagramSocket(port);
                mTempSockets.add(socket);

                Thread thread = tempListenThread(socket, mTempSockets.size() - 1);
                thread.start();

            } catch (SocketException e) {
                Log.e("ezturner", e.toString());
            }


            if (!mIsDeciding) {
                //Then start a timer to prompt the UI
            }

            mTempMasters.add(master);
        }
    }

    private void broadcastMasters(){
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("master-discovered");
        // You can also include some extra data.

        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private Thread tempListenThread(final DatagramSocket socket ,final int index){
        Runnable run = new Runnable() {
            @Override
            public void run() {
                tempListen(socket ,index);
            }
        };

        return new Thread(run);
    }

    private void tempListen(DatagramSocket socket , int index){
        while(mIsDeciding){
            //TODO: Figure out the actual max packet size
            DatagramPacket packet = new DatagramPacket(new byte[4096] , 4096);
            boolean failed = false;
            try {
                socket.receive(packet);
            } catch(IOException e){
                Log.e("ezturner" , e.toString());
                failed = true;
            }
            if(!failed) {
                synchronized (mTempPacketStorage) {
                    mTempPacketStorage.get(index).add(packet);
                }
            }
        }
    }

    //This is called when a master has been chosen
    public synchronized void chooseMaster(Master master){
        //Get the index
        int index = mTempMasters.indexOf(master);

        //Tell the threads that we've made our choice
        mIsDeciding = false;

        mDiscoverySocket.close();

        mListening = true;

        //Move the variables around
        mPackets = mTempPacketStorage.get(index);
        mSntpClient = mTempClients.get(index);
        mMaster = master;

        mTempPacketStorage = new ArrayList<ArrayList<DatagramPacket>>();
        mTempClients = new ArrayList<SntpClient>();
        mTempMasters = new ArrayList<Master>();

        for(int i = 0; i < mTempSockets.size(); i++){
            mTempSockets.get(i).close();
        }

        try {
            mSocket = new DatagramSocket(master.getPort());
        } catch(IOException e){
            Log.e("ezturner" , e.toString());
        }

    }


    private Thread startListeningForPackets(){
        return new Thread(){
            public void run(){
                while(mListening){

                }
            }
        };
    }

    public void startListening(){
        mActiveDiscoveryThread.start();
    }

}
