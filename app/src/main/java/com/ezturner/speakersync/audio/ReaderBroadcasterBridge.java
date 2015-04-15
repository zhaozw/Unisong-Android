package com.ezturner.speakersync.audio;

import com.ezturner.speakersync.network.master.AudioBroadcaster;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by ezturner on 3/11/2015.
 */
public class ReaderBroadcasterBridge extends ReaderBridge{

    private Queue<AudioFrame> mInputFrames;
    private boolean mInputReady;

    private Queue<AudioFrame> mOutputFrames;
    private boolean mOutputReady;


    private boolean mIsRunning;

    private AudioBroadcaster mBroadcaster;

    public ReaderBroadcasterBridge(AudioBroadcaster broadcaster){
        super();
        mBroadcaster = broadcaster;
    }

    public void stopBridge(){
        mIsRunning = false;
    }

    public void lastPacket(){
        mBroadcaster.lastPacket();
    }

    @Override
    protected void sendOutFrames(ArrayList<AudioFrame> frames){
        mBroadcaster.addFrames(frames);
    }

    public void setAudioTrackInfo(int sampleRate , int channels ,String mime , long duration ,int bitrate){
        mBroadcaster.setAudioTrackInfo(sampleRate , channels, mime , duration, bitrate);
    }

    public byte getStreamID(){
        return mBroadcaster.getStreamID();
    }
}
