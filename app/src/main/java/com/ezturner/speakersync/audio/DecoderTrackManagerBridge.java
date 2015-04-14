package com.ezturner.speakersync.audio;

import java.util.ArrayList;

/**
 * Created by ezturner on 4/14/2015.
 */
public class DecoderTrackManagerBridge extends ReaderBridge{

    private AudioTrackManager mManager;

    public DecoderTrackManagerBridge(AudioTrackManager manager){
        mManager = manager;
    }

    @Override
    protected void sendOutFrames(ArrayList<AudioFrame> frames){
        mManager.addFrames(frames);
    }
}
