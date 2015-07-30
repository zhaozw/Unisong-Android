package com.ezturner.speakersync.audio;

import java.util.ArrayList;
import java.util.List;

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

    private static AudioStatePublisher sInstance;

    public static final int IDLE = 0;
    public static final int PAUSED = 1;
    public static final int SEEK = 2;
    public static final int PLAYING = 3;
    public static final int RESUME = 4;
    public static final int NEW_SONG = 5;
    public static final int END_SONG = 6;

    //The time that we are seeking to
    private long mSeekTime;

    //The time that the song will be resumed at
    private long mResumeTime;
    private int mState;
    private byte mStreamID;

    //The boolean used to tell if a value has been updated (for pause time)
    private boolean mUpdated;
    private List<AudioObserver> mObservers;

    //Sets the state for Idle and instantiates the Observers arraylist
    public AudioStatePublisher(){
        mState = IDLE;
        mObservers = new ArrayList<>();
        mStreamID = -1;
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
        //Set the state
        if(state == RESUME || state == SEEK || state == NEW_SONG){
            mState = PLAYING;
        } else if(state == END_SONG){
            mState = IDLE;
        } else {
            mState = state;
        }

        if(mState == NEW_SONG){
            if(mStreamID >= 254){
                mStreamID = 0;
            } else {
                mStreamID++;
            }
        }
        notifyObservers(state);
    }

    public void notifyObservers(int state){

        mUpdated = false;
        for(AudioObserver observer : mObservers){
            observer.update(state);
        }

    }

    public void setState(int state){
        mState = state;
    }

    public int getState(){return mState;}

    public void setSeekTime(long time){
        mSeekTime = time;
        if(getState() == PAUSED){
            mResumeTime = time;
        }
    }

    public long getSeekTime(){
        return mSeekTime;
    }

    public long getResumeTime(){
        return mResumeTime;
    }

    public byte getStreamID(){return mStreamID;}
}

