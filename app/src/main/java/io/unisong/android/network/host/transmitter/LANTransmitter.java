package io.unisong.android.network.host.transmitter;

import android.util.Log;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.TimeManager;
import io.unisong.android.network.Client;
import io.unisong.android.network.host.MasterFECHandler;
import io.unisong.android.network.host.MasterTCPHandler;
import io.unisong.android.network.packets.FramePacket;
import io.unisong.android.network.packets.NetworkPacket;
import io.unisong.android.network.song.Song;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the Broadcast/Multicast functionality
 * Created by Ethan on 5/22/2015.
 */
public class LANTransmitter implements Transmitter{

    private final String LOG_TAG = LANTransmitter.class.getSimpleName();

    //The port the stream will run on
    private int mPort;

    //The FEC Handler
    private MasterFECHandler mMasterFECHandler;

    //The IP that the broadcast stream will be sent on
    private InetAddress mStreamIP;

    //The socket that the
    private DatagramSocket mStreamSocket;

    //The object that handles all reliability stuff
    private MasterTCPHandler mTCPHandler;

    //The shared map of AudioFrames to stream from.
    private Map<Integer, AudioFrame> mFrames;

    //The ID of the packet to be sent next
    private int mNextFrameSendID;

    private byte mStreamID;

    private boolean mStreamRunning;

    private AudioStatePublisher mAudioStatePublisher;

    //The Scheduler that handles packet send delays.
    private ScheduledExecutorService mWorker;

    private TimeManager mTimeManager;

    //The last frame in this song
    private int mLastFrameID;

    private List<Integer> mPacketsToRebroadcast;

    private UnisongSession mCurrentSession;

    //TODO: implement multicast
    public LANTransmitter(boolean multicast, UnisongSession unisongSession){
        Random random = new Random();
        mPort = CONSTANTS.STREAM_PORT_BASE + random.nextInt(CONSTANTS.PORT_RANGE);
        //TODO: Listen for other streams and ensure that you don't use the same port
        //TODO: send out 4-5 UDP packets over 50-100ms checking if we can use the port?


        mCurrentSession = UnisongSession.getInstance();
        mPacketsToRebroadcast = new ArrayList<>();

        mMasterFECHandler = new MasterFECHandler();

        //Get the singletons
        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mTimeManager = TimeManager.getInstance();

        try {
            mStreamIP = NetworkUtilities.getBroadcastAddress();
            //Start the socket for the actual stream
            mStreamSocket = new DatagramSocket();
            mTCPHandler = new MasterTCPHandler(this, unisongSession);

        } catch(IOException e){
            e.printStackTrace();
        }


        mWorker = Executors.newSingleThreadScheduledExecutor();
    }


    Runnable mSongStreamStart = new Runnable() {
        @Override
        public void run() {
            Log.d(LOG_TAG, "Stream starting!");
            startStream();
        }
    };

    private int mPacketsSentCount;

    Runnable mPacketSender = new Runnable() {
        @Override
        public void run() {


            if(!mStreamRunning){
                return;
            }

            long begin = System.currentTimeMillis();

            NetworkPacket packet;
            AudioFrame frame;


            if(!mFrames.containsKey(mNextFrameSendID)){
                Log.d(LOG_TAG , "Frame #" + mNextFrameSendID + " not found, starting to wait.");
            }
            //Wait for mFrames to contain the relevant frame
            while (!mFrames.containsKey(mNextFrameSendID)){
                synchronized (this){
                    try {
                        this.wait(1);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }


            synchronized (mFrames) {
                frame = mFrames.get(mNextFrameSendID);
            }

            if(mNextFrameSendID == 4306)    Log.d(LOG_TAG, frame.toString());
            packet = createFramePacket(frame);
            if(packet == null){
                Log.d(LOG_TAG , "Packet #" + mNextFrameSendID + " is null! AudioFrame is : " + frame);
            }

            mNextFrameSendID++;

            synchronized (mStreamSocket){
                try {
                    DatagramPacket datagramPacket = packet.getPacket();

                    if(datagramPacket == null){
                        Log.d(LOG_TAG , "The datagram packet is null for packet #" + (mNextFrameSendID -1));
                    }


                    if(!mStreamRunning){
                        return;
                    }
                    mStreamSocket.send(datagramPacket);

                    mPacketsSentCount++;
                } catch (IOException e){
                    e.printStackTrace();
                }
            }

            long delay = getDelay();


            rebroadcast();
            delay -= System.currentTimeMillis() - begin;

            if(delay < 0){
                delay = 0;
            }

            long diff = System.currentTimeMillis() - mTimeManager.getAACPlayTime(packet.getPacketID());

            if(mPacketsSentCount % 100 == 0) {

                Log.d(LOG_TAG, "mPacketsSentCount :" + mPacketsSentCount + " , delay is : " + delay);
            }

            if(mNextFrameSendID != mLastFrameID && mStreamRunning) {
                mWorker.schedule(mPacketSender , delay , TimeUnit.MILLISECONDS);
            }

        }

    };


    public void rebroadcastFrame(int packetID){
        if(mPacketsToRebroadcast.contains(packetID)){
            return;
        }

        synchronized (mPacketsToRebroadcast){
            mPacketsToRebroadcast.add(packetID);
        }
    }

    private void rebroadcast(){
        if(mPacketsToRebroadcast.size() > 0) {

            synchronized (mPacketsToRebroadcast){
                NetworkPacket packet;
                synchronized (mFrames) {
                    AudioFrame frame = mFrames.get(mPacketsToRebroadcast.get(0));;
                    packet = createFramePacket(frame);
                }

                if(packet == null){
                    Log.d(LOG_TAG , "Packet to be rebroadcast is null! #" + mPacketsToRebroadcast.get(0));
                }
                Log.d(LOG_TAG, "packet to be rebroadcast is: " + packet.toString());

                int count = 0;
                Client oneClient = null;
                List<Client> clients = mTCPHandler.getSlaves();
                synchronized (clients) {
                    for (Client client : clients){
                        if (!client.hasPacket(packet.getPacketID())) {
                            count++;
                            oneClient = client;
                        }
                    }
                }

                if(count == 0){
                    synchronized (mPacketsToRebroadcast) {
                        mPacketsToRebroadcast.remove(0);
                    }
                    rebroadcast();
                    return;
                } else if(count == 1){
                    mTCPHandler.sendFrameTCP(((FramePacket) packet).getFrame(), oneClient);
                    synchronized (mPacketsToRebroadcast) {
                        mPacketsToRebroadcast.remove(0);
                    }

                    rebroadcast();
                    return;
                }

                for(Client client : mTCPHandler.getSlaves()){
                    client.packetHasBeenRebroadcasted(packet.getPacketID());
                }

                synchronized (clients) {
                    for (Client client : clients) {
                        List<Integer> packets = client.getPacketsToBeReSent();
                        for (Integer i : packets) {
                            if (!mPacketsToRebroadcast.contains(i)) {
                                mPacketsToRebroadcast.add(i);
                            }
                        }
                    }
                }

                mPacketsToRebroadcast.remove(0);

                if(!mTCPHandler.checkSlaves(packet.getPacketID())) {
                    try{
                        synchronized (this){
                            this.wait(10);
                        }
                    }catch (InterruptedException e){

                    }

                    try {
                        mStreamSocket.send(packet.getPacket());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    int packetID = mPacketsToRebroadcast.get(0);
                    //If the checkSlaves returns true, then try again and again until we don't have it anymore
                    while(mTCPHandler.checkSlaves(packetID)){
                        mPacketsToRebroadcast.remove(0);
                        packetID = mPacketsToRebroadcast.get(0);
                    }
                }
            }
        }
    }

    public void startStream(){
        mStreamIP = NetworkUtilities.getBroadcastAddress();
        mStreamSocket.connect(mStreamIP, getPort());

        for(Client client : mTCPHandler.getSlaves()){
            client.notifyOfSongStart();
        }

        mStreamRunning = true;
        mWorker.schedule(mPacketSender, CONSTANTS.PACKET_SEND_DELAY, TimeUnit.MILLISECONDS);
    }

    //Stops the mPacketSender runnable
    private void stopStream(){
        mStreamRunning = false;
    }

    //Stops the mPacketSend runnable
    private void pause(){
        mStreamRunning = false;
    }

    @Override
    public void update(int state) {
        switch (state){

            case AudioStatePublisher.IDLE:
                stopStream();
                break;

            case AudioStatePublisher.RESUME:
                long resumeTime = mAudioStatePublisher.getResumeTime();
                resume(resumeTime);
                break;

            case AudioStatePublisher.PAUSED:
                pause();
                break;

            case AudioStatePublisher.SEEK:
                pause();
                long seekTime = mAudioStatePublisher.getSeekTime();
                seek(seekTime);
                resume(mAudioStatePublisher.getResumeTime());
                break;

        }
    }

    public int getPort(){
        return mPort;
    }

    public int getNextPacketSendID(){
        return mNextFrameSendID;
    }

    //TODO implement a way to keep the buffer at a certain amount (say 1000ms)
    //TODO: implement and calculate this based on the bitrate and whatnot for the FEC
    private long getDelay(){
        return 17;
    }


    public void seek(long seekTime){
        mTCPHandler.seek(seekTime);
        mNextFrameSendID = (int)(seekTime / (1024000.0 / 44100.0));
    }


    public void resume(long resumeTime){
        long newSongStartTime = System.currentTimeMillis() - resumeTime + 1000 + mTimeManager.getOffset();
        Log.d(LOG_TAG, "Resume time is : " + resumeTime + " and newSongStartTime is : " + newSongStartTime);
        mTimeManager.setSongStartTime(newSongStartTime);
    }

    private FramePacket createFramePacket(AudioFrame frame){
        return new FramePacket(frame ,getStreamID() , frame.getID());
    }

    private byte getStreamID(){
        return mStreamID;
    }

    public void setLastFrame(int lastFrame){
        mLastFrameID = lastFrame;
    }

    @Override
    public void startSong(Song song) {

    }

    public void startSong(){
        mTCPHandler.startSong();
        startStream();
    }
}
