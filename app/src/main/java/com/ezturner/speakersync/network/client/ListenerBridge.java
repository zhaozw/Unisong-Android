package com.ezturner.speakersync.network.client;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.slave.SlaveDecoder;
import com.ezturner.speakersync.audio.AbstractBridge;

import java.util.ArrayList;

/**
 * Created by Ethan on 3/12/2015.
 */
public class ListenerBridge extends AbstractBridge {

    private final String LOG_TAG = "ListenerBridge";
    private SlaveDecoder mSlaveDecoder;
    private AudioTrackManager mManager;

    public ListenerBridge(SlaveDecoder slaveDecoder, AudioTrackManager manager){
        super();
        mSlaveDecoder = slaveDecoder;
        mManager = manager;
    }

    public AudioTrackManager getManager(){
        return mManager;
    } 

    protected void sendOutFrames(ArrayList<AudioFrame> frames){
        if(mSlaveDecoder != null) {
            mSlaveDecoder.addFrames(frames);
        }
    }

    public void setDecoder(SlaveDecoder decoder){
        mSlaveDecoder = decoder;
    }

    public void startSong(long startTime , int currentPacket){
        if(mSlaveDecoder != null && mManager != null){
            mSlaveDecoder.newSong();
            mManager.newSong();
            mSlaveDecoder.decode(currentPacket , startTime);
            mSlaveDecoder.setSongStartTime(startTime);
            mManager.startSong();
        }
    }

    public void lastPacket(){
        mManager.lastPacket();
    }

    public void pause(){
        mManager.pause();
    }

    public void resume(long resumeTime){
        mManager.resume(resumeTime);
    }

    public void destroy(){
        mSlaveDecoder = null;
        mManager = null;
        super.destroy();
    }

    public void seek(long seekTime){
        mSlaveDecoder.seek(seekTime);
        if(mManager != null){
            mManager.seek(seekTime);
        }
    }


}
