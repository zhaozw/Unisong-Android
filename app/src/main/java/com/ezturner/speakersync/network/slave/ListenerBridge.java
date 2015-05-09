package com.ezturner.speakersync.network.slave;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.slave.SlaveDecoder;
import com.ezturner.speakersync.audio.ReaderBridge;

import java.util.ArrayList;

/**
 * Created by Ethan on 3/12/2015.
 */
public class ListenerBridge extends ReaderBridge{

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
        mSlaveDecoder.decode(currentPacket);
        mSlaveDecoder.setSongStartTime(startTime);
        mManager.startSong(startTime);
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


}
