package com.ezturner.speakersync.network.master;

import android.util.Log;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioStatePublisher;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.TimeManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Ethan on 2/11/2015.
 */
public class MasterTCPHandler {

    private String LOG_TAG = MasterTCPHandler.class.getSimpleName();

    //The listener for when a client joins the session and starts a TCP handshake
    private ServerSocket mServerSocket;

    //The AudioBroadcaster that this class interfaces with
    private Broadcaster mBroadcaster;

    //The thread that'll listen to reliability packets
    private Thread mServerSocketThread;

    private List<Thread> mSocketThreads;

    private boolean mRunning;

    private TimeManager mTimeManager;

    //The random number generator for choosing which slave to have as a rebroadcaster
    private Random mRandom;
    //TODO: Implement passive listening using AudioBroadcaster.DISOVERY_PASSIVE_PORT

    private MasterTCPHandler mThis;

    //TODO: extend AudioObserver and implement
    public MasterTCPHandler(Broadcaster broadcaster){

        mTimeManager = TimeManager.getInstance();

        mThis = this;

        mBroadcaster = broadcaster;

        mRecentlyRebroadcasted = new HashMap<>();

        mRandom = new Random();

        try {
            
            mServerSocket = new ServerSocket(CONSTANTS.RELIABILITY_PORT);

        } catch(IOException e){
            e.printStackTrace();
        }
        mSocketThreads = new ArrayList<>();

        mServerSocketThread = startReliabilityConnectionListener();

        mRunning = true;
        mServerSocketThread.start();
    }

    //Starts the listener for new connections
    private Thread startReliabilityConnectionListener(){
        return new Thread(){
            public void run(){
                Log.d(LOG_TAG , "Starting to listen for sockets");
                while(mRunning){
                    Socket socket = null;

                    try {
                        socket = mServerSocket.accept();
                    } catch(IOException e){
                        e.printStackTrace();
                        return;
                    }
                    //TODO: uncomment after you
//                    if(socket == null){
//                        break;
//                    }
                    Log.d(LOG_TAG , "Socket connected : " + socket.getInetAddress());

                    if(socket != null){
                        Slave newSlave = new Slave(socket.getRemoteSocketAddress().toString() , socket , mThis, mBroadcaster);
                        mBroadcaster.addSlave(newSlave);
                    }
                }
            }
        };
    }






    private Map<Integer, Long> mRecentlyRebroadcasted;

    //Checks to see if any of the slaves have the packet in question.
    public boolean checkSlaves(int packetID){
        List<Slave> slaves = mBroadcaster.getSlaves();
        //The list of slaves that have the packet in question
        List<Slave> havePacket = new ArrayList<>();

        if(checkRecentlyRebroadcasted(packetID)){
            return true;
        }

        synchronized (slaves){
            for(Slave slave : slaves) {
                if(slave.hasPacket(packetID)) {
                    havePacket.add(slave);

                }
            }



            //If no slaves have the packet return false, but if they all have it return true.
            if(havePacket.size() == 0){
                return false;
            } else if( havePacket.size() == slaves.size()){
                return true;
            }
        }



        int index = mRandom.nextInt(havePacket.size());

        Slave slave = havePacket.get(index);
        Log.d(LOG_TAG , "Telling "  + havePacket.get(index).toString() + " to rebroadcast frame #" + packetID);

        slave.retransmitPacket(packetID);

        mRecentlyRebroadcasted.put(packetID , System.currentTimeMillis());
        return true;
    }

    private boolean checkRecentlyRebroadcasted(int packetID){
        synchronized (mRecentlyRebroadcasted) {
            ArrayList<Integer> toRemove = new ArrayList<>();
            for (Map.Entry<Integer, Long> entry : mRecentlyRebroadcasted.entrySet()) {
                if (System.currentTimeMillis() - entry.getValue() >= 25) {
                    toRemove.add(entry.getKey());
                }
            }
            for(Integer i : toRemove){
                mRecentlyRebroadcasted.remove(i);
            }
        }
        if (mRecentlyRebroadcasted.containsKey(packetID)) {
            return true;
        }
        return false;
    }

    private long mSongStart;
    private int mChannels;
    private byte mStreamID;

    public void startSong(long songStart, int channels ,byte streamID ){
        mSongStart = songStart;
        mChannels = channels;
        //TODO: see about if deleting this is neccessary
        mChannels = 2;
        mStreamID = streamID;

        getStartThread().start();
    }

    private Thread getStartThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                notifyOfSongStart();
            }
        });
    }

    private void notifyOfSongStart(){
        Log.d(LOG_TAG , "Notifying all listeners of song start");
        long begin = System.currentTimeMillis();
        for(Slave slave : mBroadcaster.getSlaves()){
            slave.notifyOfSongStart();
        }
        Log.d(LOG_TAG, "Done notifying after :" + (System.currentTimeMillis() - begin) + "ms.");
    }

    //Send a TCP packet to the one that needs it containing an AAC frame
    public void sendFrameTCP(AudioFrame frame , Slave slave){
        slave.sendFrame(frame);
    }

    public synchronized void pause(){
        Log.d(LOG_TAG, "Pausing");
        for(Slave slave : mBroadcaster.getSlaves()){
            slave.pause();
        }

    }

    //Sends the resume command to all Slaves
    private void resume(long resumeTime){

        for(Slave slave : mBroadcaster.getSlaves()){
            slave.resume(resumeTime, mTimeManager.getSongStartTime());
        }

    }

    public void destroy(){
        mRunning = false;
        for (Slave slave : mBroadcaster.getSlaves()){
            slave.destroy();
        }

        try{
            mServerSocket.close();
        } catch (IOException e){
            e.printStackTrace();
        }


    }

    public void seek(long seekTime){
        for(Slave slave : mBroadcaster.getSlaves()){
            slave.seek(seekTime);
        }
    }


    //Instructs a slave to retransmit the 0 packet.
    public void retransmit(){
        mBroadcaster.getSlaves().get(0).retransmitPacket(0);
    }

    // Checks to see if any of the other Slaves can rebroadcast the packet, and if not, then
    // Tells the stream to
    public void rebroadcastPacket(int packetID){
        if(!checkSlaves(packetID)){
            mBroadcaster.rebroadcastPacket(packetID);
        }
    }

    // The function that receives updates from the AudioStatePublisher
    // This function is how we know what the user is doing in regards to pause/skip/resume
    @Override
    public void update(int state){
        switch (state){

            case AudioStatePublisher.IDLE:
                stopStream();
                break;

            case AudioStatePublisher.RESUME:
                long resumeTime = mAudioStatePublisher.getResumeTime();
                resume(resumeTime);
                break;

            case AudioStatePublisher.PAUSED:
                break;

            case AudioStatePublisher.SKIPPING:
                long seekTime = mAudioStatePublisher.getSeekTime();
                seek(seekTime);
                resume(mAudioStatePublisher.getResumeTime());
                break;
        }
    }
}
