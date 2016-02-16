package io.unisong.android.network.host;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.audio_track.AudioTrackManager;
import io.unisong.android.audio.song.Song;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.host.transmitter.Transmitter;
import io.unisong.android.network.ntp.TimeManager;
import io.unisong.android.network.session.UnisongSession;

/**
 * Created by Ethan on 2/8/2015.
 */
public class Broadcaster implements AudioObserver{

    public static final String LOG_TAG = Broadcaster.class.getSimpleName();

    private static Broadcaster instance;

    public static Broadcaster getInstance(){
        return instance;
    }
    //The Transmitters that transmit the audio data to their relevant destinations
    private List<Transmitter> transmitters;
    private TimeManager timeManager;

    private AudioStatePublisher mAudioStatePublisher;

    private UnisongSession mUnisongSession;


    private Handler handler;
    private AudioTrackManager audioTrackManager;

    private Song currentSong;

    //Makes an Broadcaster object
    public Broadcaster(UnisongSession session){
        //Get the singleton objects.
        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mAudioStatePublisher.attach(this);
        mAudioStatePublisher.setBroadcaster(this);

        timeManager = TimeManager.getInstance();
        mUnisongSession = session;

        audioTrackManager = AudioTrackManager.getInstance();

        //TODO: check to see if we're on wifi. If not, then don't start LANTransmitter
        transmitters = new ArrayList<>();
        // TODO : uncomment after server tests
        //mLANTransmitter = new LANTransmitter(false , mUnisongSession);
        //transmitters.add(mLANTransmitter);

        Log.d(LOG_TAG , "Broadcaster Created!");
        try {
            handler = new Handler();
        } catch (RuntimeException e){
            Looper.prepare();
            handler = new Handler();
        }
        instance = this;
    }

    //Starts streaming the song, starts the reliability listeners, and starts the control listener
    public void startSong(Song song){

        Log.d(LOG_TAG , "Starting Song stream");

        try {

            currentSong = song;

            timeManager.setSongStartTime(System.currentTimeMillis() + CONSTANTS.SONG_START_DELAY);

        } catch (Exception e){
            e.printStackTrace();
        }

        Log.d(LOG_TAG, "Starting broadcaster succeeded.");
    }

    Runnable mNotifyAudioPublisher = new Runnable() {
        @Override
        public void run() {
            mAudioStatePublisher.update(AudioStatePublisher.PLAYING);
        }
    };

    public void addTransmitter(Transmitter transmitter){
        transmitters.add(transmitter);
    }

    public List<Transmitter> getTransmitters(){
        return transmitters;
    }

    @Override
    public void update(int state){
        switch (state){
            case AudioStatePublisher.START_SONG:
                startSong(currentSong);
                break;
        }
    }

    //TODO: fix this up when rearchitecturing is done
    // lol rearchitecturing is never done.
    public void destroy(){
        for(Transmitter transmitter : transmitters){
            transmitter.destroy();
        }

        audioTrackManager = null;
        timeManager = null;


        instance = null;
        currentSong = null;
        handler = null;

    }
}