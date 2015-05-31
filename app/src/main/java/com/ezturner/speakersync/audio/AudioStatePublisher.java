package com.ezturner.speakersync.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * An Implementation of the Observer/Subject design pattern, this is the publisher
 * that will broadcast state updates about the application to its subscribers
 *
 * In practice this class is a mix between a publisher/subscriber class and facade.
 * Whenever an operation is to be performed regarding the audio state, this will allow for
 * a simple way to both propagate state changes and provide a simple interface to
 *
 * This class will handle the AudioState
 * Created by ezturner on 5/27/2015.
 */
public class AudioStatePublisher {

    private static AudioStatePublisher sInstance;

    public static final int IDLE = 0;
    public static final int PAUSED = 1;
    public static final int SKIPPING = 2;
    public static final int PLAYING = 3;
    public static final int RESUME = 4;

    private long mSeekTime;
    private long mPauseTime;
    private int mState;

    //The boolean used to tell if a value has been updated (for pause time)
    private boolean mUpdated;
    private List<AudioObserver> mObservers;

    //Sets the state for Idle and instantiates the Observers arraylist
    public AudioStatePublisher(){
        mState = IDLE;
        mObservers = new ArrayList<>();
    }

    public static AudioStatePublisher getInstance(){
        if(sInstance == null){
            sInstance = new AudioStatePublisher();
        }
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
        mState = state;
        notifyObservers();
    }

    public void notifyObservers(){

        if(mState == PAUSED || mState == SKIPPING){
            mPauseTime = AudioTrackManager.getLastFrameTime();
        }

        mUpdated = false;
        for(AudioObserver observer : mObservers){
            observer.update(mState);
        }

        if(mState == RESUME || mState == SKIPPING){
            mState = PLAYING;
        }
    }

    public void setPauseTime(long pauseTime){
        mPauseTime = pauseTime;
    }

    public void setState(int state){
        mState = state;
    }

    public void setSeekTime(long time){
        mSeekTime = time;
    }

    public long getSeekTime(){
        return mSeekTime;
    }

    public long getResumeTime(){
        return mPauseTime;
    }
}

