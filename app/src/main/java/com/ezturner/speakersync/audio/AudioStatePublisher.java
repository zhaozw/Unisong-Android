package com.ezturner.speakersync.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * An Implementation of the Observer/Subject design pattern, this is the publisher
 * that will broadcast state updates about the application to its subscribers
 *
 * This class will handle the AudioState
 * Created by ezturner on 5/27/2015.
 */
public class AudioStatePublisher {

    public final int IDLE = 0;
    public final int PAUSED = 1;
    public final int SKIPPING = 2;
    public final int PLAYING = 3;

    private long mSkipTime;
    private int mState;
    private List<AudioObserver> mObservers;

    //Sets the state for Idle and instantiates the Observers arraylist
    public AudioStatePublisher(){
        mState = IDLE;
        mObservers = new ArrayList<>();
    }

    //Attaches a AudioObserver to this AudioStateSubject
    public void attach(AudioObserver observer){
        mObservers.add(observer);
    }

    public void detach(AudioObserver observer){
        mObservers.add(observer);
    }

    public void notifyObservers(){
        for(AudioObserver observer : mObservers){
            observer.update(mState);
        }
    }

    public void setState(int state){
        mState = state;
    }

    public void setSkipTime(long time){
        mSkipTime = time;
    }

    public long getSkipTime(){
        return mSkipTime;
    }
}
