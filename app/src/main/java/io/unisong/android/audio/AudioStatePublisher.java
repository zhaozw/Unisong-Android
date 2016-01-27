package io.unisong.android.audio;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.host.Broadcaster;
import io.unisong.android.network.ntp.TimeManager;

/**
 * An Implementation of the Observer/Subject design pattern, this is the publisher
 * that will broadcast state updates about the application to its subscribers
 *
 * In practice this class is a mix between a publisher/subscriber class and facade.
 * Whenever an operation is to be performed regarding the audio state, this will allow for
 * a simple way to both propagate state changes and provide a simple interface to provide all
 * relevant information to the variety of classes that will need it, without having to modify large interfaces
 * every time an architecture change is made.
 *
 * This class will handle the Audio State (playing, pausing, skipping, resuming, new song)
 *
 * To use this class, first update any relevant information with the setter methods and then call update()
 * Created by ezturner on 5/27/2015.
 */
public class AudioStatePublisher {

    private static final String LOG_TAG = AudioStatePublisher.class.getSimpleName();

    private static AudioStatePublisher sInstance;

    public static final int IDLE = 0;
    public static final int PAUSED = 1;
    public static final int SEEK = 2;
    public static final int PLAYING = 3;
    public static final int RESUME = 4;
    public static final int END_SONG = 5;
    public static final int START_SONG = 6;

    private Broadcaster mBroadcaster;
    private boolean mSocketIOConfigured;
    //The time that we are seeking to
    private long mSeekTime;

    //The time that the song will be resumed at
    private long mResumeTime;
    private int mSongToEnd;
    private int mState;
    private Handler mHandler;

    //The boolean used to tell if a value has been updated (for pause time)
    private boolean mUpdated;
    private List<AudioObserver> mObservers;

    private AudioTrackManager mManager;

    //Sets the state for Idle and instantiates the Observers arraylist
    public AudioStatePublisher(){
        mHandler = new Handler();
        mState = IDLE;
        mObservers = new ArrayList<>();
        mSocketIOConfigured = false;
        sInstance = this;
    }

    public static AudioStatePublisher getInstance(){
        return sInstance;
    }

    //Attaches a AudioObserver to this AudioStateSubject
    public void attach(AudioObserver observer){
        mObservers.add(observer);
    }

    public void detach(AudioObserver observer){
        mObservers.remove(observer);
    }

    public void update(int state){
        //Set the state
        if(state == RESUME || state == SEEK ){
            mState = PLAYING;
        } else {
            mState = state;
        }

        if(state == PAUSED && mResumeTime != 0){
            // If I put this into the constructor it causes a loop of instantiation between
            // AudioTrackManager and this class due to their dual singleton design pattern.
            if(mManager == null){
                mManager = AudioTrackManager.getInstance();
            }
            mResumeTime = mManager.getLastFrameTime();
        }

        // set the songStartTime first
        if(mBroadcaster != null){
            mBroadcaster.update(state);
            mObservers.remove(mObservers.indexOf(mBroadcaster));
        }

        notifyObservers(state);
    }

    public void notifyObservers(int state){
        mUpdated = false;
        for(AudioObserver observer : mObservers) {
            observer.update(state);
        }

        if(mBroadcaster != null)
            mObservers.add(mBroadcaster);
    }

    public void setState(int state){
        mState = state;
    }

    public long getResumeTime(){
        return mResumeTime;
    }

    public long getSeekTime(){
        return mSeekTime;
    }

    public int getState(){return mState;}

    /**
     * The Seek method. It updates the seek time and then executes an update()
     */
    public void seek(long time){
        // TODO : if we are playing, pause then resume
        if(mState == PLAYING){
            update(AudioStatePublisher.PAUSED);
        }
        mSeekTime = time;
        mResumeTime = time;
        update(AudioStatePublisher.SEEK);
        mHandler.postDelayed( () ->{
            resume(mResumeTime);
        } , 5);
    }

    /**
     * The Resume method, which updates the resume time and then executes an update()
     */
    public void resume(long resumeTime){
        mResumeTime = resumeTime;
        TimeManager timeManager = TimeManager.getInstance();
        long newStartTime = System.currentTimeMillis() - resumeTime + CONSTANTS.RESUME_DELAY;
        timeManager.setSongStartTime(newStartTime);
        Log.d(LOG_TAG , "Setting new songStartTime to : " + new Date(newStartTime).toString());
        update(AudioStatePublisher.RESUME);
    }

    public void pause(){
        Log.d(LOG_TAG, "Pausing");
        update(AudioStatePublisher.PAUSED);
    }

    public void startSong(){
        mResumeTime = 0;
        update(AudioStatePublisher.START_SONG);
        update(AudioStatePublisher.PLAYING);
    }

    public void play(){
        mResumeTime = 0;
        update(AudioStatePublisher.PLAYING);
    }

    public void endSong(int songID){
        mResumeTime = 0;
        mSongToEnd = songID;
        update(AudioStatePublisher.END_SONG);
    }

    public int getSongToEnd(){
        return mSongToEnd;
    }

    /**
     * This method is so that the songStartTime can be set before notifying all of the other
     * AudioSubscribers
     */
    public void setBroadcaster(Broadcaster broadcaster){
        mBroadcaster = broadcaster;
    }


    public void destroy(){
        mObservers = new ArrayList<>();
        mManager = null;
        mBroadcaster = null;
    }
}