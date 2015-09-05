package io.unisong.android.network.master;

import android.util.Log;

import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.packets.MasterResponsePacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

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

    //Whether the master has been chosen and the class is now listening to it, or not.
    private boolean mRunning;

    //The boolean that tells whether the slave has recieved a master and is awaiting others
    private boolean mIsDeciding;

    //The boolean used to tell if the timer that waits 75ms
    private boolean mHasRecievedDiscoverPacket;

    //The socket for listening for discovery packets
    private DatagramSocket mDiscoverySocket;

    private int mPort;

    public MasterDiscoveryHandler(int port){

        //TODO : ensure that they are the same version and put some version checks in the discovery packets
        mPort = port;
        mRunning = true;

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

    private Thread getBroadcastThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                broadcast();
            }
        });
    }

    private void broadcast(){
        while(mRunning){

            MasterResponsePacket resPacket = new MasterResponsePacket(mPort);

            byte[] data = resPacket.getData();

            //Make the packet
            DatagramPacket outPacket = new DatagramPacket(data, data.length, NetworkUtilities.getBroadcastAddress(), CONSTANTS.DISCOVERY_SLAVE_PORT);

            //Send out packet
            try {
                synchronized (mSendSocket) {
                    mSendSocket.send(outPacket);
                }



            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try{
                synchronized (this){
                    this.wait(100);
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
    private Thread startPacketListener(){
        return new Thread(){
            public void run(){
                //sendStartPacket();
                Log.d(LOG_TAG, "Discovery listener started.");

                 while(mRunning){
                    listenForPacket();
                }
            }
        };
    }

    //Listens for packets
    private void listenForPacket(){
        byte[] data = new byte[256];
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
    private void handlePacket(DatagramPacket packet) {

        Log.d(LOG_TAG, "Packet received , from : " +  packet.getAddress().toString());

        MasterResponsePacket resPacket = new MasterResponsePacket(mPort);

        byte[] data = resPacket.getData();

        //Make the packet
        DatagramPacket outPacket = new DatagramPacket(data, data.length, NetworkUtilities.getBroadcastAddress(), CONSTANTS.DISCOVERY_SLAVE_PORT);

        synchronized (mSendSocket) {
            //Send out packet
            try {
                mSendSocket.send(outPacket);

                try {
                    synchronized (this) {
                        this.wait(5);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mSendSocket.send(outPacket);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop(){
        mRunning = false;
    }

    public synchronized void destroy(){
        mRunning = false;
        mReceiveSocket.close();
        mSendSocket.close();

    }


}
