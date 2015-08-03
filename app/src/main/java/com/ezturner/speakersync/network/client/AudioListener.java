package com.ezturner.speakersync.network.client;

import android.content.Context;
import android.util.Log;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioObserver;
import com.ezturner.speakersync.audio.AudioStatePublisher;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.slave.SlaveDecoder;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.TimeManager;
import com.ezturner.speakersync.network.packets.FramePacket;
import com.ezturner.speakersync.network.packets.NetworkPacket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;


/**
 * Created by Ethan on 2/8/2015.
 * This class handles listening to and audio stream. Its component discovery handler handles
 * discovery, and the reliability handler handles reliability
 *
 */
public class AudioListener implements AudioObserver{


    private final static String LOG_TAG = "AudioListener";

    //The boolean indicating whether we are listening to a stream
    private boolean mIsListening;

    //the socket for receiving the stream
    private DatagramSocket mSocket;

    //The ArrayList of received packets
    private Map<Integer , NetworkPacket> mPackets;

    //The master that the listener is currently listening to
    private Master mMaster;

    //The port that the stream will be on
    private int mPort;

    //The discovery handler, which will handle finding and choosing the
    private ClientDiscoveryHandler mSlaveDiscoveryHandler;

    //The Client reliability handler which handles packet reliability
    private ClientTCPHandler mClientTCPHandler;

    //The activity context
    private Context mContext;

    //The stream ID
    private byte mStreamID;

    //The time that the song starts at
    private long mStartTime;

    //The boolean that tells the processing thread that the packets are ready
    private boolean mPacketsReady;

    //The thread where the socket listens for packets
    private Thread mListenThread;

    //The thread that processes the packets
    private Thread mProcessingThread;

    private Queue<DatagramPacket> mProcessingQueue;

    private int mSampleRate = -1;
    private int mChannels;
    private String mMime;
    private int mBitrate;

    private SlaveDecoder mSlaveDecoder;

    private int mFirstFrame;

    //The class that handles all of the time operations
    private TimeManager mTimeManager;

    //The class that tells us what the current audio state is
    private AudioStatePublisher mAudioStatePublisher;

    //The bridge that handles communication between this class and the slave decoder and audio manager
    //TODO : get rid of this.
    private ListenerBridge mBridge;

    private AudioTrackManager mManager;

    //TODO: when receiving from the server, hold on to the AAC data just in case we do a skip backwards to save on bandwidth and battery.
    public AudioListener(){

        mManager = AudioTrackManager.getInstance();

        mTimeManager = TimeManager.getInstance();

        mSlaveDecoder = new SlaveDecoder(2, (byte)0);

        mBridge = new ListenerBridge(mSlaveDecoder , mManager);

        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mAudioStatePublisher.attach(this);

        Log.d(LOG_TAG, "Audio Listener Started");

        mSlaveDiscoveryHandler = new ClientDiscoveryHandler(this);

        mIsListening = false;

        mProcessingQueue = new LinkedList<>();

    }


    //Start playing from a master, start listening to the stream
    public void playFromMaster(Master master){
        mStartTime = -1;
        mStartSongReceived = false;

        Log.d(LOG_TAG , "Listening from master: " + master.getIP().toString().substring(1) + ":"  + master.getPort());

        mPackets = convertPackets(master.getPackets());
        mMaster = master;

        mPort = master.getPort();
        mIsListening = true;

        mSocket = master.getSocket();

        mClientTCPHandler = new ClientTCPHandler(master.getIP() , master.getPort() , this );

        mListenThread = getListenThread();
        mListenThread.start();

        mProcessingThread = getProcessingThread();
        mProcessingThread.start();


    }

    //Starts the process of finding masters
    public void findMasters() {
        mSlaveDiscoveryHandler.findMasters();
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
                case CONSTANTS.FRAME_PACKET_ID:
                    networkPacket = handleFramePacket(packet);
                    break;
            }
        //}
        if(networkPacket != null) {
//            Log.d(LOG_TAG , networkPacket.toString());
            mClientTCPHandler.packetReceived(networkPacket.getPacketID());
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

        AudioFrame frame = new AudioFrame(fp.getData(), fp.getFrameID()  , fp.getPlayTime() , mStreamID);

//        Log.d(LOG_TAG , "Frame Packet #" + fp.getPacketID() +" received.");

        mBridge.addFrame(frame);
        return fp;
    }

    private boolean songStarted = false;

    public void startSong(long startTime , int channels , byte streamID , int currentPacket){

        //TODO: get rid of this, it's just test code
        if(!songStarted){
            songStarted = true;
        } else {
            Log.d(LOG_TAG , "Song is being started a second time!");
        }

        mTimeManager.setSongStartTime(startTime);
        mStartSongReceived = true;
        mStreamID = streamID;
        mStartTime = startTime;
        mChannels = channels;

        mStartSongReceived = true;


        mFirstFrame = currentPacket;

        mBridge.startSong(mStartTime, currentPacket);
    }


    private boolean mStartSongReceived = false;


    public void setTrackBridge(ListenerBridge bridge){
        mBridge = bridge;
    }

    public DatagramPacket getPacket(int ID){
        synchronized (mPackets){
            return mPackets.get(ID).getPacket();
        }
    }

    public void addFrame(AudioFrame frame){
        mBridge.addFrame(frame);
        mCounter++;
    }

    public void resume(long resumeTime , long newSongStartTime){
        mTimeManager.setSongStartTime(newSongStartTime);
//        mAudioStatePublisher.
    }

    //TODO: implement this
    //Ends the current song, either in preparation for another or not
    public void endSong(){

    }

    public synchronized void destroy() {
        mClientTCPHandler.destroy();
        mSocket.close();
        mSlaveDiscoveryHandler.destroy();
        mSlaveDecoder.destroy();
        mIsListening = false;

        mBridge.destroy();
    }

    @Override
    public void update(int state){

    }
}
