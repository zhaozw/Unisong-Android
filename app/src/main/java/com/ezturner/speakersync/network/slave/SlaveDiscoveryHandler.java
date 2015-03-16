package com.ezturner.speakersync.network.slave;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.ntp.SntpClient;
import com.ezturner.speakersync.network.packets.MasterResponsePacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by ezturner on 2/16/2015.
 */
public class SlaveDiscoveryHandler {

    private static final String LOG_TAG = "SlaveDiscoveryHandler";
    //The active listening socket
    private DatagramSocket mSendSocket;

    //The passive listening socket
    private DatagramSocket mReceiveSocket;

    //The thread for active discovery for the save
    private Thread mDiscoveryThread;

    //The ArrayLists for when there are two clients and you want to buffer for both of them
    private ArrayList<Master> mTempMasters;

    //The application context
    private Context mContext;

    //Whether this is listening/deciding
    private boolean mIsDeciding;
    private boolean mListening;

    //The handler for choosing a master
    private Handler mHandler;

    //The AudioListener parent
    private AudioListener mParent;



    //TODO: ensure that this works with multiple masters
    public SlaveDiscoveryHandler(AudioListener parent, Context context){
        mContext = context;
        mParent = parent;


        mTempMasters = new ArrayList<Master>();

        //Makes the handler for broadcasting packets
        mHandler = new Handler();

        mIsDeciding = false;
        try {
            mReceiveSocket = new DatagramSocket(CONSTANTS.DISCOVERY_SLAVE_PORT);
            mReceiveSocket.setBroadcast(true);
            mSendSocket = new DatagramSocket(CONSTANTS.DISCOVERY_MASTER_PORT);
            mSendSocket.setBroadcast(true);
        } catch(SocketException e){
            e.printStackTrace();
        }
        Log.d(LOG_TAG , "Slave Discovery Handler started");
        findMasters();
    }

    //Listen for a discovery packet, and if you get one start listening and modify the UI to ask the user
    //if they want to play the stream
    private synchronized void listenForResponse(){
        while(mListening){

            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

            try {
                Log.d(LOG_TAG , "Listening Started");
                if(!mReceiveSocket.isClosed()) {
                    mReceiveSocket.receive(packet);
                } else {
                    return;
                }
                Log.d(LOG_TAG , "Received");
            } catch (IOException e){
                Log.d(LOG_TAG ,e.toString());
            }

            Log.d(LOG_TAG , "Packet received!");
            if(mSendSocket.isClosed()){
                Log.d(LOG_TAG , "Socket closed");
                return;
            }

            MasterResponsePacket pack = new MasterResponsePacket(packet.getData());
            int port = pack.getPort();

            Log.d(LOG_TAG , "Master Discovered! Port is: " + port);
            InetAddress addr = packet.getAddress();


            //TODO: Get phone number from this. Needs to be tested with a real phone to see what the format of the phone number
            //TODO: string is and how long it is, and more importantly how many bytes it takes up

            Master master = new Master(port, "4252414577", addr);

            boolean contains = false;

            for(Master listMaster : mTempMasters){
                if(master.getIP() == listMaster.getIP()){
                    contains = true;
                }
            }

            //Checks to see if we've already recieved this master so we don't get duplicates
            if (!contains) {
                //Creates an SntpClient
                mTempMasters.add(master);

                master.addClient(new SntpClient(addr.toString() , mParent));

                    try {
                        DatagramSocket socket = new DatagramSocket(port);
                        socket.setBroadcast(true);
                        master.setSocket(socket);

                        Thread thread = tempListenThread(mTempMasters.indexOf(master));
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
            Log.d(LOG_TAG , "Wait is over");
            mSendSocket.close();
            mReceiveSocket.close();
            mIsDeciding = false;
            mListening = false;
            if(mTempMasters.size() == 1){
                Log.d(LOG_TAG , "Only one master detected, playing from" + mTempMasters.get(0).getIP() + ":" + mTempMasters.get(0).getPort());
                mParent.playFromMaster(mTempMasters.get(0));
            }
        }
    };

    public void findMasters(){
        if(mSendSocket.isClosed() && mReceiveSocket.isClosed()) {
            try {
                mReceiveSocket = new DatagramSocket(CONSTANTS.DISCOVERY_SLAVE_PORT);
                mReceiveSocket.setBroadcast(true);
                mSendSocket = new DatagramSocket(CONSTANTS.DISCOVERY_MASTER_PORT);
                mSendSocket.setBroadcast(true);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        mDiscoveryThread = getDiscoveryThread();
        mDiscoveryThread.start();
    }

    //Sends a request
    private synchronized void sendDiscoveryRequest(){

        DatagramPacket packet = new DatagramPacket(new byte[8], 8, NetworkUtilities.getBroadcastAddress(), CONSTANTS.DISCOVERY_MASTER_PORT);


        try {
            mSendSocket.send(packet);
            Log.d(LOG_TAG , "Packet sent");
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
                mTempMasters.get(index).getSocket().receive(packet);
            } catch(IOException e){
                e.printStackTrace();
                failed = true;
            }
            if(!failed) {
                mTempMasters.get(index).addPacket(packet);
            }
        }
    }

    //This is called when a master has been chosen
    public synchronized void chooseMaster(Master master){
        //Get the index
        int index = mTempMasters.indexOf(master);

        //Tell the threads that we've made our choice
        mIsDeciding = false;
        mListening = false;
        mSendSocket.close();
        mReceiveSocket.close();

        mParent.playFromMaster(master);

        for(int i = 0; i < mTempMasters.size(); i++){
            if(i != index) {
                mTempMasters.get(i).closeSocket();
            }
        }

        mTempMasters = new ArrayList<Master>();

    }
}
