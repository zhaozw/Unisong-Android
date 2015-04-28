package com.ezturner.speakersync.audio;

import com.ezturner.speakersync.network.master.AudioBroadcaster;

import java.util.ArrayList;

/**
 * Created by Ethan on 4/27/2015.
 */
public class BroadcasterBridge extends ReaderBridge {

    private AudioBroadcaster mBroadcaster;

    public BroadcasterBridge(AudioBroadcaster broadcaster){
        mBroadcaster = broadcaster;
    }

    @Override
    protected void sendOutFrames(ArrayList<AudioFrame> frames) {
        mBroadcaster.addFrames(frames);
    }
}
