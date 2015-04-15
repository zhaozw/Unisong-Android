package com.ezturner.speakersync.audio;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by ezturner on 3/11/2015.
 */
public class TrackManagerBridge extends ReaderBridge{


    private String LOG_TAG = "TrackManagerBridge";

    private AudioTrackManager mManager;

    public TrackManagerBridge(AudioTrackManager manager){
        super();
        mManager = manager;
    }

    @Override
    public void addFrame(AudioFrame frame){
        Log.d(LOG_TAG , "Frame added");
        synchronized(mInputFrames){
            mInputFrames.add(frame);
        }
        mInputReady = true;
        synchronized(mInputThread) {
            mInputThread.notify();
        }
    }

    @Override
    protected void sendOutFrames(ArrayList<AudioFrame> frames){
        Log.d(LOG_TAG  , "wtf");
        mManager.addFrames(frames);
    }

    @Override
    protected void sendAllOutputFrames(){
        Log.d(LOG_TAG , "Sending output frames");
        mOutputReady = false;
        ArrayList<AudioFrame> frames = new ArrayList<AudioFrame>();
        synchronized (mOutputFrames){
            while(!mOutputFrames.isEmpty()){
                frames.add(mOutputFrames.poll());
            }
        }
        sendOutFrames(frames);

    }

    public void createAudioTrack(int sampleRate , int channels){
        mManager.createAudioTrack(sampleRate , channels);
    }

    public void startSong(long startTime){
        mManager.startSong(startTime);
    }

    public void lastPacket(){
        mManager.lastPacket();
    }

}
