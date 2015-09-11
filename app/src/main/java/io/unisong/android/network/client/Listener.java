package io.unisong.android.network.client;

import android.content.Context;
import android.util.Log;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.AudioTrackManager;
import io.unisong.android.audio.client.SongDecoder;
import io.unisong.android.network.HttpClient;
import io.unisong.android.network.Master;
import io.unisong.android.network.Session;
import io.unisong.android.network.TimeManager;
import io.unisong.android.network.client.receiver.LANReceiver;
import io.unisong.android.network.client.receiver.ServerReceiver;

import java.net.DatagramPacket;


/**
 * Created by Ethan on 2/8/2015.
 * This class handles listening to and audio stream. Its component discovery handler handles
 * discovery, and the reliability handler handles reliability
 *
 */
public class Listener{


    private final static String LOG_TAG = "Listener";

    //The discovery handler, which will handle finding and choosing the
    private ClientDiscoveryHandler mSlaveDiscoveryHandler;

    //The Client reliability handler which handles packet reliability
    private ClientTCPHandler mClientTCPHandler;

    //The activity context
    private Context mContext;

    //The stream ID
    private byte mStreamID;

    //The class that handles all of the time operations
    private TimeManager mTimeManager;

    //The class that tells us what the current audio state is
    private AudioStatePublisher mAudioStatePublisher;

    private AudioTrackManager mManager;

    private LANReceiver mLANReceiver;
    private ServerReceiver mServerReceiever;

    private SongDecoder mSongDecoder;

    private Session mCurrentSession;

    //TODO: when receiving from the server, hold on to the AAC data just in case we do a skip backwards to save on bandwidth and battery.
    public Listener(){
        mManager = AudioTrackManager.getInstance();

        mTimeManager = TimeManager.getInstance();

        Log.d(LOG_TAG, "Audio Listener Started");

//        mSlaveDiscoveryHandler = new ClientDiscoveryHandler(this);

        HttpClient httpClient = HttpClient.getInstance();
        httpClient.login("anoaz" , "pass");
        synchronized (this){
            try{
                this.wait(500);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        mServerReceiever = new ServerReceiver();
    }


    //Start playing from a master, start listening to the stream
    public void playFromMaster(Master master){
        mClientTCPHandler = new ClientTCPHandler(master.getIP() , master.getPort() , this );

        if(mLANReceiver != null){
            mLANReceiver.playFromMaster(master);
        }
    }

    public void packetReceived(int packetID){
        mClientTCPHandler.packetReceived(packetID);
    }

    //Starts the process of finding masters
    public void findMasters() {
        mSlaveDiscoveryHandler.findMasters();
    }

    private boolean songStarted = false;

    public void startSong(long startTime , int channels , byte streamID , int currentPacket){

        //TODO: get rid of this, it's just test code
        if(!songStarted){
            songStarted = true;
        } else {
            Log.d(LOG_TAG , "Song is being started a second time!");
        }

        mSongDecoder = new SongDecoder(channels);
        mSongDecoder.decode(startTime);

        mTimeManager.setSongStartTime(startTime);
        mStreamID = streamID;

        mManager.startSong(mSongDecoder);
    }

    public DatagramPacket getPacket(int ID){
        return mLANReceiver.getPacket(ID);
    }

    public void addFrame(AudioFrame frame){
        mSongDecoder.addInputFrame(frame);
    }

    public void resume(long resumeTime,  long newSongStartTime){
        mTimeManager.setSongStartTime(newSongStartTime);
        mAudioStatePublisher.resume(resumeTime);
    }

    //TODO: implement this
    //Ends the current song, either in preparation for another or not
    public void endSong(){

    }

    public synchronized void destroy() {
        mClientTCPHandler.destroy();
        mSlaveDiscoveryHandler.destroy();

        if(mLANReceiver != null){
            mLANReceiver.destroy();
        }
    }

}
