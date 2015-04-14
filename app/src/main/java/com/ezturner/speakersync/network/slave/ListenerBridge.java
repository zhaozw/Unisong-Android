package com.ezturner.speakersync.network.slave;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.Decoder;
import com.ezturner.speakersync.audio.ReaderBridge;

import java.util.ArrayList;

/**
 * Created by Ethan on 3/12/2015.
 */
public class ListenerBridge extends ReaderBridge{

    private Decoder mDecoder;
    private AudioTrackManager mManager;

    public ListenerBridge(Decoder decoder, AudioTrackManager manager){
        super();
        mDecoder = decoder;
        mManager = manager;
    }



    protected void sendOutFrames(ArrayList<AudioFrame> frames){
        mDecoder.addFrames(frames);
    }

    public void createAudioTrack(int sampleRate , int channels){
        mManager.createAudioTrack(sampleRate, channels);
    }

    public void startSong(long startTime){
        mManager.startSong(startTime);
    }

    public void lastPacket(){
        mManager.lastPacket();
        mDecoder.lastPacket();
    }

}
