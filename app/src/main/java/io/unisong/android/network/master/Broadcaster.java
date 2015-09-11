package io.unisong.android.network.master;

import io.unisong.android.MediaService;
import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.master.AACEncoder;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.Session;
import io.unisong.android.network.Song;
import io.unisong.android.network.TimeManager;
import io.unisong.android.network.master.transmitter.LANTransmitter;
import io.unisong.android.network.master.transmitter.ServerTransmitter;
import io.unisong.android.network.master.transmitter.Transmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ethan on 2/8/2015.
 */
public class Broadcaster implements AudioObserver {

    public static final String LOG_TAG ="AudioBroadcaster";

    //True if the listeners are running, false otherwise
    private boolean mStreamRunning;

    //The class that handles song start times and offsets.
    private TimeManager mTimeManager;

    //The Transmitters that transmit the audio data to their relevant destinations
    private List<Transmitter> mTransmitters;

    private AudioStatePublisher mAudioStatePublisher;

    private AACEncoder mEncoder;

    //The Scheduler that handles packet send delays.
    private ScheduledExecutorService mWorker;

    private LANTransmitter mLANTransmitter;

    private Session mSession;

    private ServerTransmitter mServerTransmitter;

    //Makes an Broadcaster object
    public Broadcaster(){
        //Get the singleton objects.
        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mTimeManager = TimeManager.getInstance();

        mSession = new Session();

        //TODO: check to see if we're on wifi. If not, then don't start LANTransmitter
        mTransmitters = new ArrayList<>();
        // TODO : uncomment after server tests
        //mLANTransmitter = new LANTransmitter(false , mSession);
        //mTransmitters.add(mLANTransmitter);

        mServerTransmitter = new ServerTransmitter();
        mTransmitters.add(mServerTransmitter);

        mWorker = Executors.newSingleThreadScheduledExecutor();

        mWorker.schedule(mSongStreamStart, 5000, TimeUnit.MILLISECONDS);

    }

    Runnable mSongStreamStart = new Runnable() {
        @Override
        public void run() {
            startSongStream();
        }
    };


    //Starts streaming the song, starts the reliability listeners, and starts the control listener
    public void startSongStream(){

        mEncoder = new AACEncoder();
        mEncoder.encode(0, mSession.getCurrentSongID() , MediaService.TEST_FILE_PATH);
        //TODO: actually switch the songs

        //The start time in milliseconds
        mTimeManager.setSongStartTime(System.currentTimeMillis() + CONSTANTS.SONG_START_DELAY + mTimeManager.getOffset());

        mWorker.schedule(mNotifyAudioPublisher , CONSTANTS.SONG_START_DELAY, TimeUnit.MILLISECONDS);

        for(Transmitter transmitter : mTransmitters){
            transmitter.setAACEncoder(mEncoder);
            transmitter.startSong();
        }

        mStreamRunning = true;
    }
    Runnable mNotifyAudioPublisher = new Runnable() {
        @Override
        public void run() {
            mAudioStatePublisher.update(AudioStatePublisher.PLAYING);
        }
    };

    //TODO: fix this up when rearchitecturing is done
    public void destroy(){

    }

    private void seek(long time){
        //TODO: figure out how to handle seek with the new AACEncoder/FileDecoder system.
    }


    @Override
    public void update(int state) {
        switch (state){
            case AudioStatePublisher.SEEK:
                seek(mAudioStatePublisher.getSeekTime());
                break;
        }
    }

    public void startSong(Song song){

    }
}