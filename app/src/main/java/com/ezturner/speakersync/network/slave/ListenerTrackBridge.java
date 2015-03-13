package com.ezturner.speakersync.network.slave;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.ReaderBridge;

import java.util.ArrayList;

/**
 * Created by Ethan on 3/12/2015.
 */
public class ListenerTrackBridge extends ReaderBridge{

    private AudioTrackManager mManager;

    public ListenerTrackBridge(AudioTrackManager manager){
        super();
        mManager = manager;
    }



    @Override
    protected void sendOutFrames(ArrayList<AudioFrame> frames){
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
