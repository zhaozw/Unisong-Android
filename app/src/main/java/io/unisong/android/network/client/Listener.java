package io.unisong.android.network.client;

import android.content.Context;
import android.util.Log;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.AudioTrackManager;
import io.unisong.android.audio.client.SongDecoder;
import io.unisong.android.network.Host;
import io.unisong.android.network.session.UnisongSession;
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
    private UnisongSession mSession;

    private UnisongSession mCurrentUnisongSession;

    //TODO: when receiving from the server, hold on to the AAC data just in case we do a skip backwards to save on bandwidth and battery.
    public Listener(){
        mManager = AudioTrackManager.getInstance();

        mTimeManager = TimeManager.getInstance();

        Log.d(LOG_TAG, "Audio Listener Started");

//        mSlaveDiscoveryHandler = new ClientDiscoveryHandler(this);

        synchronized (this){
            try{
                this.wait(500);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        mServerReceiever = new ServerReceiver(this);

        mSession = UnisongSession.getInstance();
    }


    //Start playing from a host, start listening to the stream
    public void playFromMaster(Host host){
        mClientTCPHandler = new ClientTCPHandler(host.getIP() , host.getPort() , this );

        if(mLANReceiver != null){
            mLANReceiver.playFromMaster(host);
        }
    }

    public void packetReceived(int packetID){
        mClientTCPHandler.packetReceived(packetID);
    }

    private boolean songStarted = false;

    public void startSong(long startTime , int channels , int songID){

        //TODO: get rid of this, it's just test code
        if(!songStarted){
            songStarted = true;
        } else {
            Log.d(LOG_TAG , "UISong is being started a second time!");
        }

        //TODO : calculate the current frame to play?
        //mSongDecoder = new SongDecoder(channels);

        // TODO : seek time
        mSongDecoder.decode(0);

        mTimeManager.setSongStartTime(startTime);
        mSession.startSong(songID);

        mManager.startSong(mSongDecoder);
    }

    public DatagramPacket getPacket(int ID){
        return mLANReceiver.getPacket(ID);
    }

    public void addFrame(AudioFrame frame){
        mManager.addInputFrame(frame);
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

        if(mLANReceiver != null){
            mLANReceiver.destroy();
        }
    }

}
