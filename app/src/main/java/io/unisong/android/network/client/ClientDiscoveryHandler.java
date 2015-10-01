package io.unisong.android.network.client;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import io.unisong.android.network.Host;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.packets.DiscoveryPacket;
import io.unisong.android.network.packets.MasterResponsePacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * Created by ezturner on 2/16/2015.
 */
public class ClientDiscoveryHandler {

    private static final String LOG_TAG = ClientDiscoveryHandler.class.getSimpleName();
    //The active listening socket
    private DatagramSocket mSocket;

    //The thread for active discovery for the save
    private Thread mDiscoveryThread;

    //The ArrayLists for when there are two clients and you want to buffer for both of them
    private ArrayList<Host> mTempHosts;

    //The application context
    private Context mContext;

    //Whether this is listening/deciding
    private boolean mIsDeciding;
    private boolean mListening;

    //The handler for choosing a master
    private Handler mHandler;

    //The Listener parent
    private Listener mParent;



    //TODO: ensure that this works with multiple masters
    public ClientDiscoveryHandler(Listener parent){
        mParent = parent;

        mTempHosts = new ArrayList<>();

        //Makes the handler for broadcasting packets
        mHandler = new Handler();

        mIsDeciding = false;
        try {
            mSocket = new DatagramSocket(CONSTANTS.DISCOVERY_PORT);
            mSocket.setBroadcast(true);
        } catch(SocketException e){
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "Client Discovery Handler started");


        mDiscoveryThread = getDiscoveryThread();
        mDiscoveryThread.start();
    }

    public void findMasters(){
    }

    //Listen for a discovery packet, and if you get one start listening and modify the UI to ask the user
    //if they want to play the stream
    private synchronized void listenForResponse(){
        while(mListening){

            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

            try {
                Log.d(LOG_TAG , "Listening Started");
                if(!mSocket.isClosed()) {
                    mSocket.receive(packet);
                } else {
                    return;
                }
            } catch (IOException e){
                Log.d(LOG_TAG ,e.toString());
            }

            Log.d(LOG_TAG , "Packet received!");
            if(mSocket.isClosed()){
                Log.d(LOG_TAG , "Socket closed");
                return;
            }

            MasterResponsePacket pack = new MasterResponsePacket(packet.getData());
            int port = pack.getPort();

            Log.d(LOG_TAG , "Host Discovered! Port is: " + port);
            InetAddress addr = packet.getAddress();


            //TODO: Get phone number from this. Needs to be tested with a real phone to see what the format of the phone number
            //TODO: string is and how long it is, and more importantly how many bytes it takes up

            Host host = new Host(port, "4252414577", addr);

            boolean contains = false;

            for(Host listHost : mTempHosts){
                if(host.getIP().equals(listHost.getIP())){
                    contains = true;
                }
            }

            //Checks to see if we've already recieved this host so we don't get duplicates
            if (!contains) {
                //Creates an SntpClient
                mTempHosts.add(host);



                    try {
                        DatagramSocket socket = new DatagramSocket(port);
                        socket.setBroadcast(true);
                        host.setSocket(socket);

                        Thread thread = tempListenThread(mTempHosts.indexOf(host));
                        thread.start();

                } catch (SocketException e) {
                    e.printStackTrace();
                }

                Log.d(LOG_TAG , "mIsDeciding: "  + mIsDeciding);
                if (!mIsDeciding) {
                    //Then start a timer to prompt the UI if multiple masters are detected
                    // or to just play from the one detected if otherwise
                    //TODO: Prompt the UI if multiple are discovered
                    mIsDeciding = true;
                    Log.d(LOG_TAG , "Awaiting more masters");
                    mHandler.postDelayed(mWaitForMoreMasters , 25 );
                }

            }
        }
    }

    Runnable mWaitForMoreMasters = new Runnable() {
        @Override
        public void run() {
            mRunnableRunning = true;
            Log.d(LOG_TAG , "Wait is over");
            mSendSocket.close();
            mReceiveSocket.close();
            mIsDeciding = false;
            mListening = false;
            if(mTempHosts.size() == 1){
                Log.d(LOG_TAG , "Only one master detected, playing from" + mTempHosts.get(0).getIP() + ":" + mTempHosts.get(0).getPort());
                mParent.playFromMaster(mTempHosts.get(0));
            }
            mRunnableRunning = false;
        }
    };



    //Sends a request
    private void sendDiscoveryRequest(){

        // TODO : disable this if not connected to wifi
        DiscoveryPacket packet = new DiscoveryPacket(NetworkUtilities.getBroadcastAddress().getAddress());

        try {
            synchronized (mSendSocket) {
                mSendSocket.send(packet.getPacket());
            }
            synchronized (this){
                try {
                    this.wait(20);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            synchronized (mSendSocket) {
                mSendSocket.send(packet.getPacket());
            }
            Log.d(LOG_TAG, "Packet sent");
        } catch(IOException e){
            e.printStackTrace();
        }

        listenForResponse();

    }

    private Thread getDiscoveryThread(){
        return new Thread(){
            public void run(){
                mListening = true;
                Log.d(LOG_TAG , "Active Discovery thread started");
                sendDiscoveryRequest();
            }
        };
    }

    private Thread tempListenThread(final int index){
        Runnable run = new Runnable() {
            @Override
            public void run() {
                tempListen(index);
            }
        };

        return new Thread(run);
    }

    private synchronized void tempListen( int index){
        while(mIsDeciding){
            //TODO: Figure out the actual max packet size
            DatagramPacket packet = new DatagramPacket(new byte[1024] , 1024);
            boolean failed = false;
            try {
                mTempHosts.get(index).getSocket().receive(packet);
            } catch(IOException e){
                e.printStackTrace();
                failed = true;
            }
            if(!failed) {
                mTempHosts.get(index).addPacket(packet);
            }
        }
    }

    //This is called when a host has been chosen
    public synchronized void chooseMaster(Host host){
        //Get the index
        int index = mTempHosts.indexOf(host);

        //Tell the threads that we've made our choice
        mIsDeciding = false;
        mListening = false;
        mSendSocket.close();
        mReceiveSocket.close();

        mParent.playFromMaster(host);

        for(int i = 0; i < mTempHosts.size(); i++){
            if(i != index){
                mTempHosts.get(i).closeSocket();
            }
        }

        mTempHosts = new ArrayList<Host>();

    }


    public synchronized void destroy(){
        mListening = false;

        mReceiveSocket.close();
        mSendSocket.close();

        if(mRunnableRunning){
            while(mRunnableRunning){
                synchronized (this){
                    try {
                        this.wait(1);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }

        mHandler.removeCallbacks(mWaitForMoreMasters);
    }
}
