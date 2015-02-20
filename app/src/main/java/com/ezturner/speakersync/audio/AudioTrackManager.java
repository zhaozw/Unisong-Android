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
    private int mFrameToPlay;

    //boolean telling the Write thread to continue or not
    private boolean mIsPlaying;

    //The thread that the writing will go on
    private Thread mWriteThread;

    //TODO: Make AudioTrack configuration dynamic with the details of the file
    public AudioTrackManager(){

        mFrames = new TreeMap<Integer , AudioFrame>();

        mIsPlaying = false;

        mFrameToPlay = 1;

        mWriteThread = getWriteThread();
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
            byte[] data = nextData();
            mAudioTrack.write(data , 0 , data.length);
        }
    }

    private byte[] nextData(){
        byte[] data = mFrames.get(mFrameToPlay).getData();
        mFrameToPlay++;
        return data;
    }

    public void stopPlaying(){
        mIsPlaying = false;
    }

    public void startPlaying(){
        mAudioTrack.play();
        mIsPlaying = true;
        mWriteThread.start();
    }

    public void createAudioTrack(int sampleRate){

        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
    }

}