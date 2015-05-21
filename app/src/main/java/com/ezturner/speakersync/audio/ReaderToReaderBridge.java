package com.ezturner.speakersync.audio;

import android.media.MediaFormat;

import com.ezturner.speakersync.audio.master.AACEncoder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by Ethan on 4/28/2015.
 */
public class ReaderToReaderBridge extends AbstractBridge {

    private AACEncoder mEncoder;
    private MediaFormat mInputFormat;
    private boolean mSeek;



    public ReaderToReaderBridge(AACEncoder encoder){
        mEncoder = encoder;
        mSeek = false;
    }

    @Override
    protected void sendOutFrames(ArrayList<AudioFrame> frames) {
        if(mEncoder != null && !mSeek) {
            mEncoder.addFrames(frames);
        }
    }

    public void destroy(){
        if(mEncoder != null) {
            mEncoder = null;
        }
    }

    public void encode(MediaFormat inputFormat, int frame){
        mEncoder.encode(inputFormat , frame , 0);
        mInputFormat = inputFormat;
    }

    public void seek(int currentID, long seekTime, byte streamID){
        mSeek = true;

        mEncoder.seek();
        Map<Integer, AudioFrame> frames = mEncoder.getFrames();
        mEncoder = new AACEncoder(mEncoder.getBroadcasterBridge() , mEncoder.getLastFrame(), streamID, frames);

        mEncoder.encode(mInputFormat , currentID , seekTime);
        mSeek = false;
    }

    public void lastFrame(int frameID){
        mEncoder.lastFrame(frameID);
    }

}
