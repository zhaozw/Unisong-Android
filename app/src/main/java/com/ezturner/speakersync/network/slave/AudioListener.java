package com.ezturner.speakersync.network.slave;

import android.content.Context;
import android.util.Log;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.SlaveDecoder;
import com.ezturner.speakersync.audio.TrackManagerBridge;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.ntp.SntpClient;
import com.ezturner.speakersync.network.packets.AudioDataPacket;
import com.ezturner.speakersync.network.packets.FramePacket;
import com.ezturner.speakersync.network.packets.NetworkPacket;
import com.ezturner.speakersync.network.packets.SongStartPacket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


/**
 * Created by Ethan on 2/8/2015.
 * This class handles listening to and audio stream. Its component discovery handler handles
 * discovery, and the reliability handler handles reliability
 *
 */
public class AudioListener {


    private final static String LOG_TAG = "AudioListener";

    //The boolean indicating whether the above objects are in use(a master has not been chosen yet)
    private boolean mIsDeciding;

    //The boolean indicating whether we are listening to a stream
    private boolean mIsListening;

    //The Sntp client for time synchronization
    private SntpClient mSntpClient;

    //The time offset between this client and the master
    private long mTimeOffset;

    //The multicast socket for discovery and control
    private MulticastSocket mManagementSocket;

    //the socket for receiving the stream
    private DatagramSocket mSocket;

    //The ArrayList of received packets
    private Map<Integer , NetworkPacket> mPackets;

    //The master that the listener is currently listening to
    private Master mMaster;

    //The broadcast address that will be used
    private InetAddress mAddress;

    //The port that the stream will be on
    private int mPort;

    //The bridge between this and the AudioTrackManager
    private ListenerBridge mBridge;

    //The discovery handler, which will handle finding and choosing the
    private SlaveDiscoveryHandler mSlaveDiscoveryHandler;

    //The Slave reliability handler which handles packet reliability
    private SlaveReliabilityHandler mSlaveReliabilityHandler;

    //The activity context
    private Context mContext;

    //The unfinished AudioFrames that need to be built and sent over to the AudioTrackManager
    private Map<Integer , AudioFrame> mUnfinishedFrames;

    private ArrayList<AudioFrame> mUnOffsetedFrames;

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

    //The InputStream used to write the mp3 data
    private NetworkInputStream mInputStream;

    //The next packet to be writted to the inputStream
    private int mPacketToWrite;

    private SlaveDecoder mSlaveDecoder;

    public AudioListener(Context context ,  ListenerBridge bridge){

        mBridge = bridge;

        mTimeOffset = -9999;
        Log.d(LOG_TAG , "Audio Listener Started");
        mContext = context;

        mSlaveDiscoveryHandler = new SlaveDiscoveryHandler(this , mContext);

        mAddress = NetworkUtilities.getBroadcastAddress();

        mIsListening = false;


        mUnOffsetedFrames = new ArrayList<AudioFrame>();

        mProcessingQueue = new LinkedList<DatagramPacket>();
    }


    //Start playing from a master, start listening to the stream
    public void playFromMaster(Master master){
        mTimeOffset = -9999;
        mStartTime = -1;
        mStartSongReceived = false;
        mPacketToWrite = 1;

        Log.d(LOG_TAG , "Listening from master: " + master.getIP().toString().substring(1) + ":"  + master.getPort());
        mSntpClient = master.getClient();
        mPackets = convertPackets(master.getPackets());
        mMaster = master;

        if(mSntpClient.hasOffset()){
            Log.d(LOG_TAG , "Offset is available!");
            mTimeOffset = (long) mSntpClient.getOffset();
            //TODO: consider whether to uncomment and remove the code applying it below
            mBridge.setOffset(mTimeOffset);
        } else {
            Log.e(LOG_TAG , "ERROR: NO OFFSET");
        }

        mPort = master.getPort();
        mIsListening = true;

        mSocket = master.getSocket();

        mSlaveReliabilityHandler = new SlaveReliabilityHandler(master.getIP() , master.getPort() , this);

        mListenThread = getListenThread();
        mListenThread.start();

        mProcessingThread = getProcessingThread();
        mProcessingThread.start();


        mUnfinishedFrames = new HashMap<Integer , AudioFrame>();
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
        DatagramPacket packet = new DatagramPacket(new byte[3048] , 3048);
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
                case CONSTANTS.SONG_START_PACKET_ID:
                    networkPacket = handleStartSongPacket(packet);
                    break;
            }
        //}
        if(networkPacket != null) {
//            Log.d(LOG_TAG , networkPacket.toString());
            mSlaveReliabilityHandler.packetReceived(networkPacket.getPacketID());
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

        AudioFrame frame = new AudioFrame(fp.getData(), fp.getFrameID() , fp.getLength() , fp.getPlayTime());

        if(mTimeOffset == -9999){

        }
        Log.d(LOG_TAG , "Frame Packet #" + fp.getPacketID() +" received.");

        mBridge.addFrame(frame);
        return fp;
    }


    private boolean mStartSongReceived = false;
    private NetworkPacket handleStartSongPacket(DatagramPacket packet){
        mStartSongReceived = true;
        SongStartPacket sp = new SongStartPacket(packet);

        mStreamID = sp.getStreamID();

        mStartTime = sp.getStartTime();

        mChannels = sp.getChannels();

        mSlaveDecoder = new SlaveDecoder(new TrackManagerBridge(mBridge.getManager()) , mChannels);
        mBridge.setDecoder(mSlaveDecoder);
        
        //TODO: this doesn't work, figure out why

        if(mMime != null){
//            mBridge.setDecoderInfo(mMime , mSampleRate , mChannels, mBitrate);
            Log.d(LOG_TAG, "Decoder Info Set");
        }
        //TODO: Figure out the time synchronization and then
        //Convert from microseconds to millis and use the Sntp offset

        Log.d(LOG_TAG, "Song start packet received! Starting song");

        if(mTimeOffset != -9999) {
            mBridge.startSong(mStartTime);
        }

        return sp;
    }



    public void setOffset(double offset){
        Log.d(LOG_TAG , "Offset is : " + offset);
        mTimeOffset = (long) offset;

        mBridge.setOffset(mTimeOffset);

        if(mStartTime != -1){
            mBridge.startSong(mStartTime);
        }

    }

    public void setTrackBridge(ListenerBridge bridge){
        mBridge = bridge;
    }

    public DatagramPacket getPacket(int ID){
        synchronized (mPackets){
            return mPackets.get(ID).getPacket();
        }
    }

}
