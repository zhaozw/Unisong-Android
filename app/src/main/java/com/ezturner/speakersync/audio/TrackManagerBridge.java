package com.ezturner.speakersync.audio;

import java.util.ArrayList;

/**
 * Created by ezturner on 3/11/2015.
 */
public class TrackManagerBridge extends AbstractBridge {


    private String LOG_TAG = "TrackManagerBridge";

    private AudioTrackManager mManager;

    public TrackManagerBridge(AudioTrackManager manager){
        super();
        mManager = manager;
    }

    @Override
    protected void sendOutFrames(ArrayList<AudioFrame> frames){
        mManager.addFrames(frames);
    }

    public void createAudioTrack(int sampleRate , int channels){
        //TODO: figure out if some decoders output PCM at a rate other than 44100hz and 2channels
//        mManager.createAudioTrack(sampleRate , channels);
    }

    public void startSong(long startTime){
        mManager.startSong();
    }

    public void lastPacket(){
        mManager.lastPacket();
    }

    public void destroy(){
        mManager = null;
        super.destroy();
    }

}
