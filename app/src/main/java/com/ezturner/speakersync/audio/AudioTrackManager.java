package com.ezturner.speakersync.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioTrackManager {

    //The AudioTrack used for playback
    private AudioTrack mAudioTrack;

    //A map of all of the frames and their IDs
    private Map<Integer , AudioFrame> mFrames;

    //the last frame ID that has been played
    private int mLastIDPlayed;

    //boolean telling the Write thread to continue or not
    private boolean mIsPlaying;

    //TODO: Make AudioTrack configuration dynamic with the details of the file
    public AudioTrackManager(){
        int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        mFrames = new TreeMap<Integer , AudioFrame>();

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        mIsPlaying = false;
    }

    public void addFrame(AudioFrame frame){
        mFrames.put(frame.getID() , frame);
    }

    public AudioFrame getFrame(int ID){
        return mFrames.get(ID);
    }

    private Thread getWriteThread(){
        return new Thread(new Runnable()  {
            public void run() {
                writeToTrack();
            }
        });
    }

    private void writeToTrack(){
        while(mIsPlaying){

        }
    }

    public void stopPlaying(){
        mIsPlaying = false;

    }


}
