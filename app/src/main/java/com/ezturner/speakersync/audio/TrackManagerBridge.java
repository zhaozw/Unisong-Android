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
    protected void sendOutFrames(ArrayList<AudioFrame> frames){
//        Log.d(LOG_TAG  , "wtf");
        mManager.addFrames(frames);
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
