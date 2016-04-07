package io.unisong.android.network.client.receiver;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.Host;
import io.unisong.android.network.client.Listener;
import io.unisong.android.network.packets.FramePacket;
import io.unisong.android.network.packets.NetworkPacket;

/**
 * The LANReceiver class listens to broadcast packets to retrieve information
 * from the local area network. Implemented in conjunction with ClientTCPHandler
 * to provide reliable multicast/broadcast functionality
 * Created by ezturner on 6/8/2015.
 */
public class LANReceiver {

    private static final String LOG_TAG = LANReceiver.class.getSimpleName();

    //the socket for receiving the stream
    private DatagramSocket socket;

    //The ArrayList of received packets
    private Map<Integer , NetworkPacket> packets;

    //The boolean indicating whether we are listening to a stream
    private boolean isListening;

    //The thread where the socket listens for packets
    private Thread listenThread;

    //The thread that processes the packets
    private Thread processingThread;

    //The queue of packets to be processed.
    private Queue<DatagramPacket> processingQueue;

    // The parent Listener which coordinates various communication methods
    private Listener parent;

    /**
     * Creates a LANReceiver with a selected Listener as its parent.
     * @param parent the Listener to utilize to coordinate with the host
     */
    public LANReceiver(Listener parent){
        this.parent = parent;
    }

    /**
     * Start listening from a selected Host
     * Begins a listening and processing thread
     * @param host the host to listen from
     */
    public void playFromMaster(Host host){

        Log.d(LOG_TAG, "Listening from host: " + host.getIP().toString().substring(1) + ":" + host.getPort());

        // retrieve any packets already stored in memory
        packets = convertPackets(host.getPackets());

        isListening = true;

        // Create the socket and queue
        socket = host.getSocket();
        processingQueue = new LinkedList<>();

        // begin listening
        listenThread = getListenThread();
        listenThread.start();

        processingThread = getProcessingThread();
        processingThread.start();
    }

    /**
     * Retrieves a thread that will listen on Unisong's port
     * @return
     */
    private Thread getListenThread(){
        return new Thread(){
            public void run(){
                Log.d(LOG_TAG, "Listening started");
                while(isListening){
                    listenForPacket();
                }
            }
        };
    }

    /**
     * Retrieves a thread that will process the queue of received
     * DatagramPackets and turn them into networkPackets for
     * @return
     */
    private Thread getProcessingThread(){
        return new Thread(){
            public void run(){
                while(isListening){

                    //Check that we are the one being notified
                    if(processingQueue.size() > 0) {

                        ArrayList<DatagramPacket> packets = new ArrayList<DatagramPacket>();
                        //long beforeSynchronized = System.currentTimeMillis();
                        synchronized (processingQueue) {
                            //long after = System.currentTimeMillis();
                            //Log.d(LOG_TAG, "Time to synchronize: " + (after - beforeSynchronized));
                            while(!processingQueue.isEmpty()) {
                                packets.add(processingQueue.poll());
                            }
                        }
                        //Log.d(LOG_TAG, "Total time to complete operation: " + (System.currentTimeMillis() - beforeSynchronized));


                        for(int i = 0; i < packets.size(); i++){
                            NetworkPacket networkPacket = handlePacket(packets.get(i));
                        }

                        //for(NetworkPacket pack : networkPackets){
                        //    Log.d(LOG_TAG , "Packet ID: " + pack);
                        //if(!packets.containsKey(pack.getPacketID())){
                        //    Log.d(LOG_TAG , "adding");
                        //    packets.put(pack.getPacketID() , pack);
                        //}
                        //}
                    }

                    try {
                        synchronized (processingThread){
                            processingThread.wait();
                        }
                    } catch (InterruptedException e){
                        //This is supposed to happen, nbd
                    }

                }
            }
        };
    }

    /**
     *
     * @param packets
     * @return
     */
    private Map<Integer , NetworkPacket> convertPackets(ArrayList<DatagramPacket> packets){
        Map<Integer , NetworkPacket> networkPackets = new HashMap<Integer , NetworkPacket>();

        for(int i = 0; i < packets.size(); i++){
            NetworkPacket packet = handlePacket(packets.get(i));
            networkPackets.put(packet.getPacketID() ,packet );
        }

        return networkPackets;
    }


    private double counter = 0;
    private double lastPacket = 0;

    private long finishTime = 0;
    private long startTime = 0;


    /**
     *
     */
    private void listenForPacket(){
        DatagramPacket packet = new DatagramPacket(new byte[1030] , 1030);
        try{
            //startTime = System.currentTimeMillis();
            //Log.d(LOG_TAG , "Time difference is : " + (startTime - finishTime));
            socket.receive(packet);
            //finishTime = System.currentTimeMillis();

        } catch(IOException e){
            e.printStackTrace();
        }



        //long before = System.currentTimeMillis();
        synchronized (processingQueue){
            processingQueue.add(packet);
        }
        //mCountsProcessing++;
        //mTotalProcessingDelay += System.currentTimeMillis() - before;

        //before = System.currentTimeMillis();
        synchronized (processingThread){
            processingThread.notify();
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

    /**
     *
     * @param packet
     * @return
     */
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
            parent.packetReceived(networkPacket.getPacketID());
            if(!packets.containsKey(networkPacket.getPacketID())){

                counter++;

                lastPacket = networkPacket.getPacketID();
                if(counter % 100 == 0){
                    double packetLoss = (lastPacket - counter) / lastPacket;
                    Log.d(LOG_TAG , "The number of datagrams received : " + counter + ", and the current packet number: " + networkPacket.getPacketID() + " which is a loss rate of : " + packetLoss);
                }
            }

            packets.put(networkPacket.getPacketID(), networkPacket);
        }
        //TODO: get rid of old packets that we don't need anymore
        return networkPacket;
    }

    /**
     *
     * @param packet
     * @return
     */
    private NetworkPacket handleFramePacket(DatagramPacket packet){

        FramePacket fp = new FramePacket(packet);

        AudioFrame frame = new AudioFrame(fp.getData(), fp.getFrameID()  , fp.getPlayTime());

        parent.addFrame(frame);
        return fp;
    }

    /**
     *
     * @param ID
     * @return
     */
    public DatagramPacket getPacket(int ID){
        synchronized (packets){
            return packets.get(ID).getPacket();
        }
    }

    /**
     *
     */
    public void destroy(){
        socket.close();
        isListening = false;
    }

}
