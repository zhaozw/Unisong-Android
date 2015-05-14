package com.ezturner.speakersync.audio;

import com.ezturner.speakersync.audio.master.AACEncoder;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Ethan on 4/28/2015.
 */
public class ReaderToReaderBridge extends AbstractBridge {

    private AACEncoder mEncoder;
    private boolean mSeek;

    public ReaderToReaderBridge(AACEncoder encoder){
        mEncoder = encoder;
        mSeek = false;
    }

    @Override
    protected void sendOutFrames(ArrayList<AudioFrame> frames) {
        if(mEncoder != null) {
            mEncoder.addFrames(frames);
        }
    }

    public void destroy(){
        if(mEncoder != null) {
            mEncoder = null;
        }
    }

    public void seek(int currentID){
        mSeek = true;
        synchronized (mFrames){
            mFrames = new LinkedList<>();
        }
        mEncoder.seek(currentID);
    }

}
