package io.unisong.android.network.host;

import android.os.Handler;
import android.util.Log;

import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.AudioTrackManager;
import io.unisong.android.audio.master.AACEncoder;
import io.unisong.android.audio.master.FileDecoder;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.song.Song;
import io.unisong.android.network.song.LocalSong;
import io.unisong.android.network.TimeManager;
import io.unisong.android.network.host.transmitter.LANTransmitter;
import io.unisong.android.network.host.transmitter.ServerTransmitter;
import io.unisong.android.network.host.transmitter.Transmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ethan on 2/8/2015.
 */
public class Broadcaster implements AudioObserver {

    public static final String LOG_TAG = Broadcaster.class.getSimpleName();

    private static Broadcaster sInstance;

    public static Broadcaster getInstance(){
        return sInstance;
    }

    //True if the listeners are running, false otherwise
    private boolean mStreamRunning;

    //The class that handles song start times and offsets.
    private TimeManager mTimeManager;

    //The Transmitters that transmit the audio data to their relevant destinations
    private List<Transmitter> mTransmitters;

    private AudioStatePublisher mAudioStatePublisher;

    private UnisongSession mUnisongSession;


    private Handler mHandler;
    private AudioTrackManager mAudioTrackManager;

    private Song mCurrentSong;

    //Makes an Broadcaster object
    public Broadcaster(UnisongSession session){
        //Get the singleton objects.
        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mAudioStatePublisher.attach(this);

        mTimeManager = TimeManager.getInstance();
        mUnisongSession = session;

        mAudioTrackManager = AudioTrackManager.getInstance();

        //TODO: check to see if we're on wifi. If not, then don't start LANTransmitter
        mTransmitters = new ArrayList<>();
        // TODO : uncomment after server tests
        //mLANTransmitter = new LANTransmitter(false , mUnisongSession);
        //mTransmitters.add(mLANTransmitter);

        ServerTransmitter serverTransmitter = new ServerTransmitter();
        mTransmitters.add(serverTransmitter);

        Log.d(LOG_TAG , "Broadcaster Created!");
        mHandler = new Handler();
        sInstance = this;
    }

    //Starts streaming the song, starts the reliability listeners, and starts the control listener
    private void startSongStream(Song song){

        Log.d(LOG_TAG , "Starting Song stream");

        try {
            mTimeManager.setSongStartTime(System.currentTimeMillis() + CONSTANTS.SONG_START_DELAY + mTimeManager.getOffset());
            mCurrentSong = song;

            if(!song.started())
                song.start();

            // The start time in milliseconds

            for (Transmitter transmitter : mTransmitters) {
                transmitter.startSong(song);
            }

            mStreamRunning = true;

        } catch (Exception e){
            e.printStackTrace();
        }

        Log.d(LOG_TAG , "Starting broadcaster succeeded.");
    }

    Runnable mNotifyAudioPublisher = new Runnable() {
        @Override
        public void run() {
            mAudioStatePublisher.update(AudioStatePublisher.PLAYING);
        }
    };

    public void addTransmitter(Transmitter transmitter){
        mTransmitters.add(transmitter);
    }

    public List<Transmitter> getTransmitters(){
        return mTransmitters;
    }

    //TODO: fix this up when rearchitecturing is done
    // lol rearchitecturing is never done.
    public void destroy(){
        for(Transmitter transmitter : mTransmitters){
            transmitter.destroy();
        }

        mAudioTrackManager = null;
        mTimeManager = null;


        sInstance = null;
        mCurrentSong = null;
        mHandler = null;

    }

    private void seek(long time){
        mCurrentSong.seek(time);
    }


    @Override
    public void update(int state){
        switch (state){
            case AudioStatePublisher.SEEK:
                seek(mAudioStatePublisher.getSeekTime());
                break;
            case AudioStatePublisher.PLAYING:
                Log.d(LOG_TAG , "plz");
                startSong(mUnisongSession.getCurrentSong());
                break;
        }
    }

    public void startSong(Song song){
        startSongStream(song);
    }
}