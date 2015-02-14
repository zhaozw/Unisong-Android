package com.ezturner.audiotracktest.network;

import android.util.Log;

import com.ezturner.audiotracktest.MainActivity;
import com.ezturner.audiotracktest.MediaService;
import com.ezturner.audiotracktest.network.ntp.SntpClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * Created by Ethan on 2/8/2015.
 */
public class AudioListener {

    private ArrayList<SntpClient> tempClients;

    private SntpClient mSntpClient;
    private MulticastSocket mManagementSocket;
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


    public AudioListener(){

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
        mActiveDiscoveryThread.start();

        mPassiveDiscoveryThread = getPassiveDiscoveryThread();
        mPassiveDiscoveryThread.start();

        mHasRecievedDiscoverPacket = false;

    }

    public void playFromMaster(Master master){

    }

    private void startListening(String serverIP){

        mSntpClient = new SntpClient(serverIP);
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
        if(! MainActivity.multicastLockIsHeld()) {
            MainActivity.aquireMulticastLock();
        }
    }


    //Stop listening to the multicast stream
    private void stopListeningMulticast(){
        if(MainActivity.multicastLockIsHeld()) {
            MainActivity.releaseMulticastLock();
        }

    }


    private Thread getActiveDiscoveryThread(){
        return new Thread(){
            public void run(){
                sendDiscoveryRequest();
            }
        };
    }

    private Thread getPassiveDiscoveryThread(){
        return new Thread(){
            public void run(){
                listenForDiscoveryResponse(false);
            }
        };
    }

    //Listen for a discovery packet, and if you get one start listening and modify the UI to ask the user
    //if they want to play the stream
    private void listenForDiscoveryResponse(boolean active){
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

        byte[] port = new byte[]{data[0 ] , data[1] , data[2] , data[3]};

        //TODO: Get phone number from this. Needs to be tested with a real phone to see what the format of the phone number
        //TODO: string is and how long it is, and more importantly how many bytes it takes up



    }



}
