package io.unisong.android.network.client;

import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.audiotrack.AudioTrackManager;
import io.unisong.android.audio.song.Song;
import io.unisong.android.network.Host;
import io.unisong.android.network.client.receiver.LANReceiver;
import io.unisong.android.network.client.receiver.Receiver;
import io.unisong.android.network.client.receiver.ServerReceiver;
import io.unisong.android.network.ntp.TimeManager;
import io.unisong.android.network.session.UnisongSession;


/**
 * Created by Ethan on 2/8/2015.
 * This class handles listening to and audio stream. Its component discovery handler handles
 * discovery, and the reliability handler handles reliability
 *
 */
public class Listener{


    private final static String LOG_TAG = Listener.class.getSimpleName();

    private static Listener sInstance;

    public static Listener getInstance(){
        return sInstance;
    }

    //The class that handles all of the time operations
    private TimeManager mTimeManager;

    //The class that tells us what the current audio state is
    private AudioStatePublisher mAudioStatePublisher;

    private AudioTrackManager mManager;

    private LANReceiver mLANReceiver;
    private ServerReceiver mServerReceiever;
    private List<Receiver> mReceivers;

    private Handler mHandler;
    private UnisongSession mSession;

    //TODO: when receiving from the server, hold on to the AAC data just in case we do a skip backwards to save on bandwidth and battery.
    public Listener(UnisongSession session){
        mManager = AudioTrackManager.getInstance();

        mSession = session;
        mTimeManager = TimeManager.getInstance();
        mAudioStatePublisher = AudioStatePublisher.getInstance();

        Log.d(LOG_TAG, "Audio Listener Started");

//        mSlaveDiscoveryHandler = new ClientDiscoveryHandler(this);

        //mServerReceiever = new ServerReceiver(this);

        // TODO : set session.
        //mSession = UnisongSession.getInstance();

        mReceivers = new ArrayList<>();
        mHandler = new Handler();

        if(sInstance != null)
            Log.d(LOG_TAG , "Listener double instantiated!");

        // Set this instance to the singleton
        sInstance = this;
    }


    //Start playing from a host, start listening to the stream
    public void playFromMaster(Host host){

        if(mLANReceiver != null){
            mLANReceiver.playFromMaster(host);
        }
    }

    public void joinServerSession(int sessionID){
        mServerReceiever.joinSession(sessionID);
    }

    public void packetReceived(int packetID){

    }

    //
    private long mLastUpdate = 0;

    /**
     * This will start a song with the
     * @param startTime
     * @param songID
     */
    public void startSong(long startTime , int songID){

        //TODO : calculate the current frame to play?
        //mSongDecoder = new SongDecoder(channels);

        mTimeManager.setSongStartTime(startTime - TimeManager.getInstance().getOffset());

        Song song = mSession.getSongQueue().getSong(songID);
        if(song == null){
            Log.d(LOG_TAG , "We do not have the song! Update the session!");
            if(System.currentTimeMillis() - mLastUpdate >= 1000) {
                mSession.getUpdate();
                mLastUpdate = System.currentTimeMillis();
            }

            mHandler.postDelayed(() -> {
                startSong(startTime, songID);
            } , 100);
        } else {

            AudioStatePublisher.getInstance().startSong();
        }

    }

    public void addReceiver(Receiver receiver){
        mReceivers.add(receiver);
    }

    public DatagramPacket getPacket(int ID){
        return mLANReceiver.getPacket(ID);
    }

    public void deleteSong(int songID){
        mSession.deleteSong(songID);
    }

    public void addSong(Song song){
        mSession.addSong(song);
    }

    public void updateSong(JSONObject object){
        mSession.updateSong(object);
    }

    public void addFrame(AudioFrame frame){
        mSession.addFrame(frame);
    }

    public void seek(long seekTime){
        mAudioStatePublisher.seek(seekTime);
    }

    public void pause(){
        mAudioStatePublisher.pause();
    }

    public void play(){
        mAudioStatePublisher.play();
    }

    public void resume(long resumeTime,  long newSongStartTime){
        mTimeManager.setSongStartTime(newSongStartTime);
        mAudioStatePublisher.resume(resumeTime);
    }

    //Ends the current song, either in preparation for another or not
    public void endSong(int songID){
        mAudioStatePublisher.endSong(songID);
    }

    public synchronized void destroy() {
        //mClientTCPHandler.destroy();

        if(mLANReceiver != null){
            mLANReceiver.destroy();
        }
    }

}
