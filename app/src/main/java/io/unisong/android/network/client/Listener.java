package io.unisong.android.network.client;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.audio_track.AudioTrackManager;
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

    private static Listener instance;

    public static Listener getInstance(){
        return instance;
    }

    //The class that handles all of the time operations
    private TimeManager timeManager;

    //The class that tells us what the current audio state is
    private AudioStatePublisher audioStatePublisher;

    private AudioTrackManager manager;
    private Integer dataToRequestTo = Integer.MAX_VALUE;

    private LANReceiver mLANReceiver;
    private ServerReceiver mServerReceiever;
    private List<Receiver> receivers;

    private Handler handler;
    private UnisongSession session;

    //TODO: when receiving from the server, hold on to the AAC data just in case we do a skip backwards to save on bandwidth and battery.
    public Listener(UnisongSession session){
        manager = AudioTrackManager.getInstance();

        this.session = session;
        timeManager = TimeManager.getInstance();
        audioStatePublisher = AudioStatePublisher.getInstance();

        Log.d(LOG_TAG, "Audio Listener Started");

//        mSlaveDiscoveryHandler = new ClientDiscoveryHandler(this);

        //mServerReceiever = new ServerReceiver(this);

        // TODO : set session.
        //session = UnisongSession.getInstance();

        receivers = new ArrayList<>();
        try {
            handler = new Handler();
        } catch (RuntimeException e){
            Looper.prepare();
            handler = new Handler();
        }

        if(instance != null)
            Log.d(LOG_TAG , "Listener double instantiated!");

        // Set this instance to the singleton
        instance = this;
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
        //mSongDecoder = new UnisongDecoder(channels);

        timeManager.setSongStartTime(startTime - TimeManager.getInstance().getOffset());

        Song song = session.getSongQueue().getSong(songID);
        if(song == null){
            Log.d(LOG_TAG , "We do not have the song! Update the session!");
            if(System.currentTimeMillis() - mLastUpdate >= 1000) {
                session.getUpdate();
                mLastUpdate = System.currentTimeMillis();
            }

            handler.postDelayed(() -> {
                startSong(startTime, songID);
            }, 100);
        } else {

            AudioStatePublisher.getInstance().startSong();
        }

    }

    public void addReceiver(Receiver receiver){
        receivers.add(receiver);
    }

    public DatagramPacket getPacket(int ID){
        return mLANReceiver.getPacket(ID);
    }

    public void deleteSong(int songID){
        session.getSongQueue().deleteSong(songID);
    }

    public void addSong(Song song){
        session.addSong(song);
    }

    public void updateSong(JSONObject object){
        session.updateSong(object);
    }

    public void addFrame(AudioFrame frame){
        session.addFrame(frame);
    }

    public void seek(long seekTime){
        Log.d(LOG_TAG , "Seek received!");
        audioStatePublisher.seek(seekTime);
    }

    public void pause(){
        Log.d(LOG_TAG , "Pause received!");
        audioStatePublisher.pause();
    }

    public void play(){
        Log.d(LOG_TAG , "Play received!");
        audioStatePublisher.play();
    }

    public void resume(long resumeTime,  long newSongStartTime){
        Log.d(LOG_TAG , "Resume received!");
        timeManager.setSongStartTime(newSongStartTime - timeManager.getOffset());
        audioStatePublisher.resume(resumeTime);
    }

    //Ends the current song, either in preparation for another or not
    public void endSong(int songID){
        audioStatePublisher.endSong(songID);
    }

    public void destroy() {
        for(Receiver receiver : receivers){
            receiver.destroy();
        }

        receivers = null;

        instance = null;
    }

    /**
     * This begins requesting data required to play the song, and then subsequently
     * adjusts the requests based on data received.
     */
    public void requestData(Song song){
        int currentFrame = (int) ((timeManager.getSongTime()) / (1024000.0 / 44100.0));

        if(receivers.size() == 1){
            Receiver receiver = receivers.get(0);

            long duration = song.getDuration();

            // request all of the data between now and the end of the song
            receiver.requestData(song , currentFrame - 1 , (int)((1024.0 / song.getFormat().getSampleRate() / 1000.0) * duration ));

        } else {
            // TODO : implement for Omnius
        }
    }

}
