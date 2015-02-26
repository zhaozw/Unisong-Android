package com.ezturner.speakersync.network.slave;

import android.content.Context;
import android.util.Log;

import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.master.AudioBroadcaster;
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
 * Created by ezturner on 2/16/2015.
 */
public class SlaveDiscoveryHandler {

    //The active listening socket
    private DatagramSocket mSocket;

    //The passive listening socket
    private DatagramSocket mPassiveSocket;

    //The thread for active discovery for the save
    private Thread mActiveDiscoveryThread;

    //The thread for active discovery for the save
    private Thread mPassiveDiscoveryThread;

    //The ArrayLists for when there are two clients and you want to buffer for both of them
    private ArrayList<SntpClient> mTempClients;
    private ArrayList<DatagramSocket> mTempSockets;
    private ArrayList<ArrayList<DatagramPacket>> mTempPacketStorage;
    private ArrayList<Master> mTempMasters;

    //The application context
    private Context mContext;

    //Whether

    private boolean mIsDeciding;

    private boolean mListening;

    //The AudioListener parent
    private AudioListener mParent;

    public SlaveDiscoveryHandler(AudioListener parent, Context context){
        mContext = context;
        mParent = parent;

        mTempClients = new ArrayList<SntpClient>();

        mTempSockets = new ArrayList<DatagramSocket>();

        mTempPacketStorage = new ArrayList<ArrayList<DatagramPacket>>();

        mIsDeciding = false;
        try {
            mPassiveSocket = new DatagramSocket(AudioBroadcaster.DISCOVERY_PASSIVE_PORT);
        } catch(SocketException e){
            e.printStackTrace();
        }
    }

    //Listen for a discovery packet, and if you get one start listening and modify the UI to ask the user
    //if they want to play the stream
    private synchronized void listenForResponse(boolean active){
        while(mListening) {
            DatagramPacket packet = new DatagramPacket(new byte[256], 256);

            if(active) {
                try {
                    mSocket.setSoTimeout(750);
                } catch (SocketException e) {
                    Log.e("ezturner-error", e.toString());
                }
            }

            try {
                if(active) {
                    mSocket.receive(packet);
                } else  {
                    mPassiveSocket.receive(packet);
                }
            } catch (IOException e) {
                Log.e("ezturner", e.toString());
            }

            if(mSocket.isClosed()){
                Log.d("ezturner" , "Socket closed");
                return;
            }


            byte[] data = packet.getData();

            InetAddress addr = packet.getAddress();

            byte[] portArr = new byte[]{data[0], data[1], data[2], data[3]};

            //Make a buffer wrapper to decode the port, then decode it
            ByteBuffer wrapped = ByteBuffer.wrap(portArr);
            int port = wrapped.getInt();

            //TODO: Get phone number from this. Needs to be tested with a real phone to see what the format of the phone number
            //TODO: string is and how long it is, and more importantly how many bytes it takes up

            Master master = new Master(port, "4252414577", addr);


            //Checks to see if we've already recieved this master so we don't get duplicates
            if (!mTempMasters.contains(master)) {
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
                    //Then start a timer to prompt the UI if multiple masters are detected
                    // or to just play from the one detected if otherwise
                }

                mTempMasters.add(master);
            }
        }
    }

    public void findMasters(){
        mActiveDiscoveryThread = getActiveDiscoveryThread();
        mActiveDiscoveryThread.start();
    }

    //Sends a request
    private synchronized void sendDiscoveryRequest(){
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(new byte[512], 512, Inet4Address.getByName("192.168.1.255"), AudioBroadcaster.DISCOVERY_PORT);
        } catch(UnknownHostException e){
            e.printStackTrace();
        }
        //mDiscoverySocket.close();

        Log.d("ezturner" , "huhhuhhuh");
        try {
            mSocket.send(packet);
            Log.d("ezturner" , "Packet sent");
        } catch(IOException e){
            Log.e("ezturner","Line 127 : " + e.toString());
        }

    }

    private Thread getActiveDiscoveryThread(){
        return new Thread(){
            public void run(){
                mIsDeciding = true;

                sendDiscoveryRequest();
            }
        };
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
            DatagramPacket packet = new DatagramPacket(new byte[1024] , 1024);
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

        mParent.playFromMaster(master, mTempPacketStorage.get(index), mTempClients.get(index));

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
}
