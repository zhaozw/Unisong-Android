package com.ezturner.speakersync.audio;

import com.ezturner.speakersync.audio.master.AACEncoder;

import java.util.ArrayList;

/**
 * Created by Ethan on 4/28/2015.
 */
public class ReaderToReaderBridge extends AbstractBridge {

    private AACEncoder mEncoder;

    public ReaderToReaderBridge(AACEncoder encoder){
        mEncoder = encoder;
    }

    @Override
    protected void sendOutFrames(ArrayList<AudioFrame> frames) {
        mEncoder.addFrames(frames);
    }

    public void destroy(){
        mEncoder = null;
    }
}
