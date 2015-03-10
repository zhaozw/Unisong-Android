package com.ezturner.speakersync.network.slave;

import android.content.Context;
import android.net.Network;
import android.provider.ContactsContract;
import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.ntp.SntpClient;
import com.ezturner.speakersync.network.packets.FrameDataPacket;
import com.ezturner.speakersync.network.packets.FrameInfoPacket;
import com.ezturner.speakersync.network.packets.NetworkPacket;
import com.ezturner.speakersync.network.packets.SongStartPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


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

    private AudioTrackManager mAudioTrackManager;

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
    private byte mStreamId;

    //The time that the song starts at
    private long mStartTime;




    public AudioListener(Context context , AudioTrackManager atm){

        Log.d(LOG_TAG , "Audio Listener Started");
        mAudioTrackManager = atm;
        mContext = context;

        mSlaveDiscoveryHandler = new SlaveDiscoveryHandler(this , mContext);

        mAddress = NetworkUtilities.getBroadcastAddress();

        mIsListening = false;


        mUnOffsetedFrames = new ArrayList<AudioFrame>();

    }

    //Start playing from a master, start listening to the stream
    public void playFromMaster(Master master){

        Log.d(LOG_TAG , "Listening from master: " + master.getIP().toString().substring(1) + ":"  + master.getPort());
        mSntpClient = master.getClient();
        mPackets = convertPackets(master.getPackets());
        mMaster = master;

        mPort = master.getPort();

        mSocket = master.getSocket();

        mSlaveReliabilityHandler = new SlaveReliabilityHandler(master.getIP());

        mUnfinishedFrames = new HashMap<Integer , AudioFrame>();
    }

    //Starts the process of finding masters
    public void findMasters() {
        mSlaveDiscoveryHandler.findMasters();
    }

    private Thread startListeningForPackets(){
        return new Thread(){
            public void run(){
                while(mIsListening){

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

    private void listenForPacket(){
        DatagramPacket packet = new DatagramPacket(new byte[1536] , 1536);

        try{
            mSocket.receive(packet);
            Log.d(LOG_TAG , "Packet Received");
        } catch(IOException e){
            e.printStackTrace();
        }
        NetworkPacket networkPacket = handlePacket(packet);
        mPackets.put(networkPacket.getPacketID() , networkPacket);

    }

    private NetworkPacket handlePacket(DatagramPacket packet){
        if(packet.getData()[1] == mStreamId) {
            switch (packet.getData()[0]) {
                case CONSTANTS.FRAME_INFO_PACKET_ID:
                    return handleFrameInfoPacket(packet);

                case CONSTANTS.FRAME_DATA_PACKET_ID:
                    return handleFrameDataPacket(packet);

                case CONSTANTS.SONG_START_PACKET_ID:
                    return handleSongSwitch(packet);
            }
        }
        return null;
    }


    public long getNextFrameWriteTime(){
        return 0;
    }

    private NetworkPacket handleFrameInfoPacket(DatagramPacket packet){
        //TODO: Check for any data frames that have arrived before the info packet
        FrameInfoPacket fp = new FrameInfoPacket(packet.getData());
        if(mPackets.containsKey(fp.getPacketID())){
            AudioFrame frame = new AudioFrame(fp.getFrameID(), fp.getNumPackets(), fp.getPlayTime(), fp.getLength(), fp.getPacketID());

            mSlaveReliabilityHandler.packetReceived(fp.getPacketID());
            mUnfinishedFrames.put(frame.getID(), frame);
        }
        return fp;
    }

    private NetworkPacket handleFrameDataPacket(DatagramPacket packet){
        FrameDataPacket fp = new FrameDataPacket(packet.getData());

        if(mUnfinishedFrames.containsKey(fp.getFrameID())){
            AudioFrame frame = mUnfinishedFrames.get(fp.getFrameID());

            boolean frameFinished = frame.addData(fp.getFrameID(), fp.getAudioFrameData());

            if (frameFinished && mTimeOffset != -1){
                frame.setOffset(mTimeOffset);
                mAudioTrackManager.addFrame(frame);
                mUnfinishedFrames.remove(fp.getFrameID());
            } else if(frameFinished){
                mUnOffsetedFrames.add(frame);
                mUnfinishedFrames.remove(fp.getFrameID());
            }
        }

        return fp;
    }

    private NetworkPacket handleSongSwitch(DatagramPacket packet){
        SongStartPacket sp = new SongStartPacket(packet.getData());

        mStreamId = sp.getStreamID();

        mStartTime = sp.getStartTime();

        //TODO: Figure out the time synchronization and then
        //Convert from microseconds to millis and use the Sntp offset

        mAudioTrackManager.startSong(mStartTime);

        return sp;
    }

    public void setOffset(double offset){
        mTimeOffset = (long) offset;
        for(int i = 0; i < mUnOffsetedFrames.size(); i++){
            AudioFrame frame = mUnOffsetedFrames.get(i);

            frame.setOffset(mTimeOffset);

            mAudioTrackManager.addFrame(frame);
        }

        mUnOffsetedFrames = new ArrayList<AudioFrame>();
    }

}
