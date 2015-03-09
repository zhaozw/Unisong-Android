package com.ezturner.speakersync.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioTrackManager {

    private String LOG_TAG = "AudioTrackManager";

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

    //The current last frame
    private int mLastFrameId;

    //The handler for scheduling resyncs and the song start
    private Handler mHandler;

    //The time difference between this and the master in milliseconds
    private double mOffset;

    //TODO: Make AudioTrack configuration dynamic with the details of the file
    public AudioTrackManager(){
        mFrames = new TreeMap<Integer , AudioFrame>();

        mIsPlaying = false;

        mFrameToPlay = 0;

        mWriteThread = getWriteThread();
        mLastFrameId = 0;

        mHandler = new Handler();
    }

    Runnable mStartSong = new Runnable() {
        @Override
        public void run() {
                if(mIsPlaying){
                    //TODO: Switch the song
                } else {
                    startPlaying();
                }

        }
    };

    public void addFrame(AudioFrame frame){
        mFrames.put(frame.getID() , frame);
        if(frame.getID() > mLastFrameId){
            mLastFrameId = frame.getID();
        }
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

    private int mTest = 0;
    private void writeToTrack(){
        try {
            Thread.sleep(500);
        } catch(InterruptedException e){
            e.printStackTrace();
        }

        while(mIsPlaying){
            if(mFrameToPlay == mLastFrameId) mIsPlaying = false;
            byte[] data = nextData();

            mTest++;

            if(mTest >= 200){
                AudioFrame frame = mFrames.get(mFrameToPlay - 1);
                long difference = System.currentTimeMillis() - frame.getPlayTime();
                Log.d(LOG_TAG , "Current time is :" + System.currentTimeMillis() + " , As opposed to a  write time of : " + frame.getPlayTime() + " , A difference of : " + difference);
            }
            mAudioTrack.write(data, 0, data.length);
        }
    }

    private byte[] nextData(){
        byte[] data  = null;
        AudioFrame frame = mFrames.get(mFrameToPlay);
        if(frame != null) {
            data = frame.getData();
            mFrameToPlay++;
        }

        return data;
    }

    public void stopPlaying(){
        mIsPlaying = false;
    }

    public void startSong(long songStart){
        double millisTillSongStart = System.currentTimeMillis() - (songStart + mOffset);
        mHandler.postDelayed(mStartSong , (long)millisTillSongStart);
    }

    public void startPlaying(){
        mAudioTrack.play();
        mIsPlaying = true;
        mWriteThread.start();
    }

    public boolean isPlaying(){
        return mIsPlaying;
    }

    public void createAudioTrack(int sampleRate){
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
    }

    public void setAudioTrack(AudioTrack track){
        mAudioTrack = track;
    }

    public void release(){
        if(mAudioTrack != null) {
            mAudioTrack.flush();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    public void setOffset(double offset){
        mOffset = offset;
    }


}