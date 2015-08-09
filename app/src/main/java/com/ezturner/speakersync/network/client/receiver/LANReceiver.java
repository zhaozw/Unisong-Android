package com.ezturner.speakersync.network.client.receiver;

import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.client.Listener;
import com.ezturner.speakersync.network.packets.FramePacket;
import com.ezturner.speakersync.network.packets.NetworkPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Created by ezturner on 6/8/2015.
 */
public class LANReceiver {

    private static final String LOG_TAG = LANReceiver.class.getSimpleName();

    //the socket for receiving the stream
    private DatagramSocket mSocket;

    //The ArrayList of received packets
    private Map<Integer , NetworkPacket> mPackets;

    //The boolean indicating whether we are listening to a stream
    private boolean mIsListening;

    //The boolean that tells the processing thread that the packets are ready
    private boolean mPacketsReady;

    //The thread where the socket listens for packets
    private Thread mListenThread;

    //The thread that processes the packets
    private Thread mProcessingThread;

    private Queue<DatagramPacket> mProcessingQueue;

    private Listener mParent;

    private int mPort;

    public LANReceiver(Listener parent){
        mParent = parent;
    }

    //Start playing from a master, start listening to the stream
    public void playFromMaster(Master master){

        Log.d(LOG_TAG, "Listening from master: " + master.getIP().toString().substring(1) + ":" + master.getPort());

        mPackets = convertPackets(master.getPackets());

        mPort = master.getPort();
        mIsListening = true;

        mSocket = master.getSocket();

        mListenThread = getListenThread();
        mListenThread.start();

        mProcessingThread = getProcessingThread();
        mProcessingThread.start();


    }
    private Thread getListenThread(){
        return new Thread(){
            public void run(){
                Log.d(LOG_TAG, "Listening started");
                while(mIsListening){
                    listenForPacket();
                }
            }
        };
    }

    private Thread getProcessingThread(){
        return new Thread(){
            public void run(){
                while(mIsListening){

                    //Check that we are the one being notified
                    if(mProcessingQueue.size() > 0) {

                        ArrayList<DatagramPacket> packets = new ArrayList<DatagramPacket>();
                        //long beforeSynchronized = System.currentTimeMillis();
                        synchronized (mProcessingQueue) {
                            //long after = System.currentTimeMillis();
                            //Log.d(LOG_TAG, "Time to synchronize: " + (after - beforeSynchronized));
                            while(!mProcessingQueue.isEmpty()) {
                                packets.add(mProcessingQueue.poll());
                            }
                        }
                        //Log.d(LOG_TAG, "Total time to complete operation: " + (System.currentTimeMillis() - beforeSynchronized));


                        for(int i = 0; i < packets.size(); i++){
                            NetworkPacket networkPacket = handlePacket(packets.get(i));
                        }

                        //for(NetworkPacket pack : networkPackets){
                        //    Log.d(LOG_TAG , "Packet ID: " + pack);
                        //if(!mPackets.containsKey(pack.getPacketID())){
                        //    Log.d(LOG_TAG , "adding");
                        //    mPackets.put(pack.getPacketID() , pack);
                        //}
                        //}
                    }

                    try {
                        synchronized (mProcessingThread){
                            mProcessingThread.wait();
                        }
                    } catch (InterruptedException e){
                        //This is supposed to happen, nbd
                    }

                }
            }
        };
    }

    //Changes the packets from DatagramPacket to
    private Map<Integer , NetworkPacket> convertPackets(ArrayList<DatagramPacket> packets){
        Map<Integer , NetworkPacket> networkPackets = new HashMap<Integer , NetworkPacket>();

        for(int i = 0; i < packets.size(); i++){
            NetworkPacket packet = handlePacket(packets.get(i));
            networkPackets.put(packet.getPacketID() ,packet );
        }

        return networkPackets;
    }


    private double mCounter = 0;
    private double mLastPacket = 0;

    private long finishTime = 0;
    private long startTime = 0;



    private void listenForPacket(){
        DatagramPacket packet = new DatagramPacket(new byte[1030] , 1030);
        try{
            //startTime = System.currentTimeMillis();
            //Log.d(LOG_TAG , "Time difference is : " + (startTime - finishTime));
            mSocket.receive(packet);
            //finishTime = System.currentTimeMillis();

        } catch(IOException e){
            e.printStackTrace();
        }



        //long before = System.currentTimeMillis();
        synchronized (mProcessingQueue){
            mProcessingQueue.add(packet);
        }
        //mCountsProcessing++;
        //mTotalProcessingDelay += System.currentTimeMillis() - before;

        //before = System.currentTimeMillis();
        synchronized (mProcessingThread){
            mProcessingThread.notify();
        }
        /*
        mNotifyCounts++;
        mTotalNotifyDelay += System.currentTimeMillis() -  before;

        if(mCountsProcessing % 100 == 0){
            Log.d(LOG_TAG , "Average Delay for Counts is : " + (mTotalProcessingDelay / mCountsProcessing));
        }

        if(mNotifyCounts % 100 == 0){
            Log.d(LOG_TAG , "Average Delay for Notify is : " + (mTotalNotifyDelay / mNotifyCounts));
        }*/
    }

    private NetworkPacket handlePacket(DatagramPacket packet){
        NetworkPacket networkPacket = null;
        //TODO: put stream ID back and implement all dat junk
        //if(packet.getData()[1] == mStreamID) {
        byte packetType = packet.getData()[0];
        switch (packetType) {
            case CONSTANTS.UDP_FRAME_PACKET_ID:
                networkPacket = handleFramePacket(packet);
                break;
        }
        //}
        if(networkPacket != null) {
//            Log.d(LOG_TAG , networkPacket.toString());
            mParent.packetReceived(networkPacket.getPacketID());
            if(!mPackets.containsKey(networkPacket.getPacketID())){

                mCounter++;

                mLastPacket = networkPacket.getPacketID();
                if(mCounter % 100 == 0){
                    double packetLoss = (mLastPacket - mCounter) / mLastPacket;
                    Log.d(LOG_TAG , "The number of datagrams received : " + mCounter + ", and the current packet number: " + networkPacket.getPacketID() + " which is a loss rate of : " + packetLoss);
                }
            }

            mPackets.put(networkPacket.getPacketID() , networkPacket);
        }
        //TODO: get rid of old packets that we don't need anymore
        return networkPacket;
    }

    private NetworkPacket handleFramePacket(DatagramPacket packet){

        FramePacket fp = new FramePacket(packet);

        AudioFrame frame = new AudioFrame(fp.getData(), fp.getFrameID()  , fp.getPlayTime());

        mParent.addFrame(frame);
        return fp;
    }

    public DatagramPacket getPacket(int ID){
        synchronized (mPackets){
            return mPackets.get(ID).getPacket();
        }
    }

    public void destroy(){
        mSocket.close();
        mIsListening = false;
    }

}
