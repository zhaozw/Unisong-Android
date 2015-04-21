package com.ezturner.speakersync.network.slave;

import android.content.Context;
import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.ntp.SntpClient;
import com.ezturner.speakersync.network.packets.FrameDataPacket;
import com.ezturner.speakersync.network.packets.FrameInfoPacket;
import com.ezturner.speakersync.network.packets.FramePacket;
import com.ezturner.speakersync.network.packets.MimePacket;
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



    public AudioListener(Context context ){

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
        mStartSongReceived = false;

        Log.d(LOG_TAG , "Listening from master: " + master.getIP().toString().substring(1) + ":"  + master.getPort());
        mSntpClient = master.getClient();
        mPackets = convertPackets(master.getPackets());
        mMaster = master;

        if(mSntpClient.hasOffset()){
            mTimeOffset = (long) mSntpClient.getOffset();
            mBridge.setOffset(mTimeOffset);
        }

        mPort = master.getPort();
        mIsListening = true;

        mSocket = master.getSocket();



        mSlaveReliabilityHandler = new SlaveReliabilityHandler(master.getIP());

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
                        ArrayList<NetworkPacket> networkPackets = new ArrayList<>();
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
                            if(networkPacket != null && mLastPacket < networkPacket.getPacketID()){
                                mLastPacket = networkPacket.getPacketID();
                            }
                            networkPackets.add(networkPacket);
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
                case CONSTANTS.FRAME_INFO_PACKET_ID:
                    networkPacket = handleFrameInfoPacket(packet);
                    break;

                case CONSTANTS.FRAME_DATA_PACKET_ID:
                    networkPacket = handleFrameDataPacket(packet);
                    break;

                case CONSTANTS.SONG_START_PACKET_ID:
                    networkPacket = handleStartSongPacket(packet);
                    break;
                case CONSTANTS.FRAME_PACKET_ID:
                    networkPacket = handleFramePacket(packet);
                    break;
                case CONSTANTS.MIME_PACKET_ID:
                    networkPacket = handleMimePacket(packet);
                    break;
            }
        //}
        if(networkPacket != null) {
            mSlaveReliabilityHandler.packetReceived(networkPacket.getPacketID());
            if(!mPackets.containsKey(networkPacket.getPacketID())){

                mCounter++;

                if(mCounter % 100 == 0){
                    Log.d(LOG_TAG , "The number of datagrams received : " + mCounter + ", and the current packet number: " + mLastPacket + " which is a loss rate of : " + ((mLastPacket - mCounter) / mLastPacket) );
                }
            }
        }
        return networkPacket;
    }


    public long getNextFrameWriteTime(){
        return 0;
    }

    private NetworkPacket handleFrameInfoPacket(DatagramPacket packet){
        //TODO: Check for any data frames that have arrived before the info packet
        FrameInfoPacket fp = new FrameInfoPacket(packet.getData());

        //.d(LOG_TAG , "Frame Info Packet for Frame " + fp.getFrameID());
        AudioFrame frame = new AudioFrame(fp.getFrameID(), fp.getNumPackets(), fp.getPlayTime(), fp.getLength(), fp.getPacketID());

        mUnfinishedFrames.put(frame.getID(), frame);

        return fp;
    }

    private NetworkPacket handleFramePacket(DatagramPacket packet){

        FramePacket fp = new FramePacket(packet.getData());

        AudioFrame frame = new AudioFrame(fp.getAudioData(), fp.getFrameID() , fp.getLength() , fp.getPlayTime());

        if(mTimeOffset == -9999){

        }
        Log.d(LOG_TAG , "Frame Packet #" + fp.getPacketID() +" received.");

        mBridge.addFrame(frame);
        return fp;
    }

    private NetworkPacket handleFrameDataPacket(DatagramPacket packet){
        FrameDataPacket fp = new FrameDataPacket(packet.getData());

        //Log.d(LOG_TAG, "handling packet");
        if(mUnfinishedFrames.containsKey(fp.getFrameID())){
            AudioFrame frame = mUnfinishedFrames.get(fp.getFrameID());

            boolean frameFinished = frame.addData(fp.getPacketID(), fp.getAudioFrameData());

            //Log.d(LOG_TAG , "FrameDataPacket Received");
            if (frameFinished && mTimeOffset != -1){
                frame.setOffset(mTimeOffset);
                Log.d(LOG_TAG , "Frame # " + frame.getID() +" reconstructed");
                //TODO : remove comment after testing
                //mBridge.addFrame(frame);
                mUnfinishedFrames.remove(fp.getFrameID());
            } else if(frameFinished){
                Log.d(LOG_TAG , "Frame Finished, but offset not set");
                mUnOffsetedFrames.add(frame);
                mUnfinishedFrames.remove(fp.getFrameID());
            }
        } else {
            //Log.d(LOG_TAG , "Frame ID of " + fp.getFrameID() + " not found");
        }

        return fp;
    }

    private boolean mStartSongReceived = false;
    private NetworkPacket handleStartSongPacket(DatagramPacket packet){
        mStartSongReceived = true;
        SongStartPacket sp = new SongStartPacket(packet.getData());

        mStreamID = sp.getStreamID();

        mStartTime = sp.getStartTime();
        
        //TODO: this doesn't work, figure out why
        mSampleRate = sp.getSampleRate();
        mChannels = sp.getChannels();
        mBitrate = sp.getBitrate();
        mBridge.createAudioTrack(sp.getSampleRate() , sp.getChannels());

        if(mMime != null){
//            mBridge.setDecoderInfo(mMime , mSampleRate , mChannels, mBitrate);
            Log.d(LOG_TAG, "Decoder Info Set");
        }
        //TODO: Figure out the time synchronization and then
        //Convert from microseconds to millis and use the Sntp offset

        Log.d(LOG_TAG , "Song start packet received! Starting song");
        //TODO: remove comment after its safe
//        mBridge.startSong(mStartTime);

        return sp;
    }

    private NetworkPacket handleMimePacket(DatagramPacket packet){
        MimePacket mp = new MimePacket(packet.getData());

        mMime = mp.getMime();
        Log.d(LOG_TAG , "Mime type : " + mMime);

        if(mStartSongReceived){
//            mBridge.setDecoderInfo(mMime , mSampleRate , mChannels, mBitrate);
            Log.d(LOG_TAG , "Decoder Info Set");
        }
        Log.d(LOG_TAG  , "Mime Packet Received");

        return mp;
    }

    public void setOffset(double offset){
        mTimeOffset = (long) offset;
        for(int i = 0; i < mUnOffsetedFrames.size(); i++){
            AudioFrame frame = mUnOffsetedFrames.get(i);

            frame.setOffset(mTimeOffset);

            mBridge.addFrame(frame);
        }
        mBridge.setOffset(mTimeOffset);

        mUnOffsetedFrames = new ArrayList<AudioFrame>();
    }

    public void setTrackBridge(ListenerBridge bridge){
        mBridge = bridge;
    }

}
