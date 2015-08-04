package com.ezturner.speakersync.network.master;

import com.ezturner.speakersync.MediaService;
import com.ezturner.speakersync.audio.AudioObserver;
import com.ezturner.speakersync.audio.AudioStatePublisher;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.master.AACEncoder;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.Session;
import com.ezturner.speakersync.network.TimeManager;
import com.ezturner.speakersync.network.master.transmitter.LANTransmitter;
import com.ezturner.speakersync.network.master.transmitter.Transmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ethan on 2/8/2015.
 */
public class Broadcaster implements AudioObserver{

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

    private int mCurrentSongID;

    private Session mSession;

    //Makes an Broadcaster object
    public Broadcaster(){
        //Get the singleton objects.
        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mTimeManager = TimeManager.getInstance();

        mSession = new Session();

        //TODO: check to see if we're on wifi. If not, then don't start LANTransmitter
        mTransmitters = new ArrayList<>();
        mLANTransmitter = new LANTransmitter(false , mSession);
        mTransmitters.add(mLANTransmitter);

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

    private void seek(){
        //TODO: figure out how to handle seek with the new AACEncoder/FileDecoder system.
    }

    @Override
    public void update(int state) {
        switch (state){
            //TODO: write a good way to switch songs
//            case AudioStatePublisher.NEW_SONG:
//                startSongStream();
//                break;
            case AudioStatePublisher.SEEK:
                seek();
                break;
        }
    }
}