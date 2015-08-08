package com.ezturner.speakersync.network.master;

import android.util.Log;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioObserver;
import com.ezturner.speakersync.audio.AudioStatePublisher;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.Client;
import com.ezturner.speakersync.network.Session;
import com.ezturner.speakersync.network.TimeManager;
import com.ezturner.speakersync.network.master.transmitter.LANTransmitter;

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
public class MasterTCPHandler implements AudioObserver {

    private String LOG_TAG = MasterTCPHandler.class.getSimpleName();

    //The singleton objects
    private TimeManager mTimeManager;
    private AudioStatePublisher mAudioStatePublisher;

    //The listener for when a client joins the session and starts a TCP handshake
    private ServerSocket mServerSocket;

    //The thread that'll listen to reliability packets
    private Thread mServerSocketThread;

    private boolean mRunning;

    //The random number generator for choosing which slave to have as a rebroadcaster
    private Random mRandom;
    //TODO: Implement passive listening using AudioBroadcaster.DISOVERY_PASSIVE_PORT

    private MasterTCPHandler mThis;

    private List<Client> mClients;

    private LANTransmitter mLANTransmitter;

    private Session mSession;


    //TODO: extend AudioObserver and implement
    public MasterTCPHandler(LANTransmitter transmitter, Session session){

        mLANTransmitter = transmitter;

        mSession = session;

        mClients = new ArrayList<>();

        mTimeManager = TimeManager.getInstance();
        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mThis = this;

        mRecentlyRebroadcasted = new HashMap<>();

        mRandom = new Random();

        try {
            
            mServerSocket = new ServerSocket(CONSTANTS.RELIABILITY_PORT);

        } catch(IOException e){
            e.printStackTrace();
        }

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
                    Socket socket;

                    try {
                        socket = mServerSocket.accept();
                    } catch(IOException e){
                        Log.d(LOG_TAG , "");
                        e.printStackTrace();
                        return;
                    }

                    if(socket == null){
                        break;
                    }
                    Log.d(LOG_TAG , "Socket connected : " + socket.getInetAddress());

                    Client newClient = new Client(socket.getRemoteSocketAddress().toString() , socket , mThis, mLANTransmitter);
                    mClients.add(newClient);
                }
            }
        };
    }

    //The packets that have recently been re-broadcasted.
    private Map<Integer, Long> mRecentlyRebroadcasted;

    //Checks to see if any of the slaves have the packet in question.
    public boolean checkSlaves(int packetID){
        //The list of slaves that have the packet in question
        List<Client> havePacket = new ArrayList<>();

        if(checkRecentlyRebroadcasted(packetID)){
            return true;
        }

        synchronized (mClients){
            for(Client client : mClients) {
                if(client.hasPacket(packetID)) {
                    havePacket.add(client);

                }
            }

            //If no slaves have the packet return false, but if they all have it return true.
            if(havePacket.size() == 0){
                return false;
            } else if( havePacket.size() == mClients.size()){
                return true;
            }
        }



        int index = mRandom.nextInt(havePacket.size());

        Client client = havePacket.get(index);
        Log.d(LOG_TAG , "Telling "  + havePacket.get(index).toString() + " to rebroadcast frame #" + packetID);

        client.retransmitPacket(packetID);

        mRecentlyRebroadcasted.put(packetID , System.currentTimeMillis());
        return true;
    }

    // This method checks to see if a packet has been recently rebroadcasted
    private boolean checkRecentlyRebroadcasted(int packetID){
        synchronized (mRecentlyRebroadcasted) {
            //Remove any that have been sent more than 45ms ago.
            ArrayList<Integer> toRemove = new ArrayList<>();
            for (Map.Entry<Integer, Long> entry : mRecentlyRebroadcasted.entrySet()) {
                if (System.currentTimeMillis() - entry.getValue() >= 45) {
                    toRemove.add(entry.getKey());
                }
            }
            for(Integer i : toRemove){
                mRecentlyRebroadcasted.remove(i);
            }
        }

        return mRecentlyRebroadcasted.containsKey(packetID);
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
        for(Client client : mClients){
            client.notifyOfSongStart();
        }
        Log.d(LOG_TAG, "Done notifying after :" + (System.currentTimeMillis() - begin) + "ms.");
    }

    //Send a TCP packet to the one that needs it containing an AAC frame
    public void sendFrameTCP(AudioFrame frame , Client client){
        client.sendFrame(frame);
    }

    public synchronized void pause(){
        Log.d(LOG_TAG, "Pausing");
        for(Client client : mClients){
            client.pause();
        }

    }

    //Sends the resume command to all Slaves
    private void resume(long resumeTime){

        for(Client client : mClients){
            client.resume(resumeTime, mTimeManager.getSongStartTime());
        }

    }

    public void destroy(){
        mRunning = false;
        for (Client client : mClients){
            client.destroy();
        }

        try{
            mServerSocket.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void seek(long seekTime){
        for(Client client : mClients){
            client.seek(seekTime);
        }
    }

    // Checks to see if any of the other Slaves can rebroadcast the packet, and if not, then
    // Tells the stream to
    public void rebroadcastFrame(int frameID){
        //TODO: figure out an elegant/reusable way to handle retransmissions
        if(!checkSlaves(frameID)){
            mLANTransmitter.rebroadcastFrame(frameID);
        }
    }

    // The function that receives updates from the AudioStatePublisher
    // This function is how we know what the user is doing in regards to pause/skip/resume
    @Override
    public void update(int state){
        switch (state){
            case AudioStatePublisher.RESUME:
                long resumeTime = mAudioStatePublisher.getResumeTime();
                resume(resumeTime);
                break;

            case AudioStatePublisher.PAUSED:
                pause();
                break;

            case AudioStatePublisher.SEEK:
                long seekTime = mAudioStatePublisher.getSeekTime();
                seek(seekTime);
                resume(seekTime);
                break;
        }
    }

    public void startSong(){
        getStartThread().start();
    }

    public List<Client> getSlaves(){
        return mClients;
    }
}
