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
    private DatagramSocket mSocket;

    //The passive listening socket
    private DatagramSocket mPassiveSocket;

    //The thread for active discovery for the save
    private Thread mActiveDiscoveryThread;

    //The thread for active discovery for the save
    private Thread mPassiveDiscoveryThread;

    //The ArrayLists for when there are two clients and you want to buffer for both of them
    private ArrayList<Master> mTempMasters;

    //The application context
    private Context mContext;

    //Whether this is listening/deciding
    private boolean mIsDeciding;
    private boolean mListening;

    //The handler for choosing a mster
    private Handler mHandler;

    //The AudioListener parent
    private AudioListener mParent;

    public SlaveDiscoveryHandler(AudioListener parent, Context context){
        mContext = context;
        mParent = parent;


        mTempMasters = new ArrayList<Master>();

        //Makes the handler for broadcasting packets
        mHandler = new Handler();

        mIsDeciding = false;
        try {
            mPassiveSocket = new DatagramSocket(CONSTANTS.DISCOVERY_SLAVE_PASSIVE_PORT);
            mPassiveSocket.setBroadcast(true);
            mSocket = new DatagramSocket(CONSTANTS.DISCOVERY_SLAVE_PORT);
            mSocket.setBroadcast(true);
        } catch(SocketException e){
            e.printStackTrace();
        }
        Log.d(LOG_TAG , "Slave Discovery Handler started");
        findMasters();
    }

    //Listen for a discovery packet, and if you get one start listening and modify the UI to ask the user
    //if they want to play the stream
    private synchronized void listenForResponse(boolean active){
        while(mListening){
            DatagramPacket packet;
            if(active) {
                packet = new DatagramPacket(new byte[1024], 1024);
            } else {
                packet = new DatagramPacket(new byte[1024] , 1024);
            }
            try {
                if(active) {
                    Log.d(LOG_TAG , "Listening Started");
                    mSocket.receive(packet);
                    Log.d(LOG_TAG , "Received");
                } else  {
                    mPassiveSocket.receive(packet);
                }
            } catch (IOException e){
                e.printStackTrace();
            }

            Log.d(LOG_TAG , "Packet received!");
            if(mSocket.isClosed()){
                Log.d(LOG_TAG , "Socket closed");
                return;
            }

            MasterResponsePacket pack = new MasterResponsePacket(packet.getData());
            int port = pack.getPort();

            Log.d(LOG_TAG , "Master Discovered! Port is: " + port);
            mIsDeciding = true;
            InetAddress addr = packet.getAddress();


            //TODO: Get phone number from this. Needs to be tested with a real phone to see what the format of the phone number
            //TODO: string is and how long it is, and more importantly how many bytes it takes up

            Master master = new Master(port, "4252414577", addr);


            //Checks to see if we've already recieved this master so we don't get duplicates
            if (!mTempMasters.contains(master)) {
                //Creates an SntpClient

                //master.addClient(new SntpClient(addr.toString()));
                    try {
                        DatagramSocket socket = new DatagramSocket(port);
                        master.setSocket(socket);

                        Thread thread = tempListenThread(mTempMasters.size() - 1);
                        thread.start();

                } catch (SocketException e) {
                    e.printStackTrace();
                }


                if (!mIsDeciding) {
                    //Then start a timer to prompt the UI if multiple masters are detected
                    // or to just play from the one detected if otherwise
                    //TODO: Prompt the UI if multiple are discovered
                    mIsDeciding = true;
                    mHandler.postDelayed(mWaitForMoreMasters , 25 );
                }

                mTempMasters.add(master);
            }
        }
    }

    Runnable mWaitForMoreMasters = new Runnable() {
        @Override
        public void run() {
            if(mTempMasters.size() == 1){
                mParent.playFromMaster(mTempMasters.get(0));
            }
        }
    };

    public void findMasters(){
        mActiveDiscoveryThread = getActiveDiscoveryThread();
        mActiveDiscoveryThread.start();

        mPassiveDiscoveryThread = getPassiveDiscoveryThread();
        mPassiveDiscoveryThread.start();
    }

    //Sends a request
    private synchronized void sendDiscoveryRequest(){
        //TODO: SET THIS TO CONSTANTS.getBroadcastAddress()
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024, NetworkUtilities.getBroadcastAddress(), CONSTANTS.DISCOVERY_MASTER_PORT);


        try {
            mSocket.send(packet);
            Log.d(LOG_TAG , "Packet sent");
        } catch(IOException e){
            e.printStackTrace();
        }

        listenForResponse(true);

    }

    private Thread getActiveDiscoveryThread(){
        return new Thread(){
            public void run(){
                mListening = true;
                Log.d(LOG_TAG , "Active Discovery thread started");
                sendDiscoveryRequest();
            }
        };
    }

    private Thread getPassiveDiscoveryThread(){
        return new Thread(){
            public void run(){
                mListening = true;
                Log.d(LOG_TAG , "Passive Discovery thread started");
                listenForResponse(false);
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

    private void tempListen( int index){
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
                synchronized (mTempMasters) {
                    mTempMasters.get(index).addPacket(packet);
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
        mListening = false;
        mSocket.close();
        mPassiveSocket.close();

        mParent.playFromMaster(master);

        for(int i = 0; i < mTempMasters.size(); i++){
            if(i != index) {
                mTempMasters.get(i).closeSocket();
            }
        }

        mTempMasters = new ArrayList<Master>();

    }
}
