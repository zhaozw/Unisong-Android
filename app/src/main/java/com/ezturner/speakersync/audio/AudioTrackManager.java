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
    private int mLastFrameID;

    //The handler for scheduling resyncs and the song start
    private Handler mHandler;

    //The time difference between this and the master in milliseconds
    private double mOffset;

    //The time that the current or next song will start at
    private long mSongStartTime;

    //The last frame added's ID
    private int mLastAddedFrameID;

    //TODO: Make AudioTrack configuration dynamic with the details of the file
    public AudioTrackManager(){
        mFrames = new TreeMap<Integer , AudioFrame>();

        mIsPlaying = false;

        mFrameToPlay = 0;

        mLastFrameID = -89;

        mHandler = new Handler();
        mOffset = 0;
    }

    Runnable mStartSong = new Runnable() {
        @Override
        public void run() {
                if(mIsPlaying){
                    //TODO: Switch the song
                } else {
                    startPlaying();
                    mHandler.post(mWriteRunnable);
                }
        }
    };

    Runnable mWriteRunnable = new Runnable() {
        @Override
        public void run() {
            if(mFrameToPlay == mLastFrameID) mIsPlaying = false;

            AudioFrame frame;
            synchronized (mFrames) {
                frame = mFrames.get(mFrameToPlay);
            }
            mFrameToPlay++;
            //TODO: handle it when this is null AND when the stream is over
            byte[] data = frame.getData();

            mTest++;

            if(mTest >= 200){
                mTest = 0;
                long difference = System.currentTimeMillis() - (frame.getPlayTime() + mSongStartTime + (long)mOffset);
                //TODO : track down bug where difference is crazy huge
                Log.d(LOG_TAG , "Time difference is : " + difference);
            }
            mAudioTrack.write(data, 0, data.length);
            long millisTillNextWrite = (long)(mSongStartTime + mOffset + frame.getPlayTime()) - System.currentTimeMillis();
            mHandler.postDelayed(mWriteRunnable , millisTillNextWrite );
        }
    };
    //Takes in some frames, then waits for mFrames to be open and writes it to it
    public void addFrames(ArrayList<AudioFrame> frames){
        synchronized (mFrames){
            for(AudioFrame frame : frames){
                int ID = frame.getID();
                mFrames.put( ID , frame);
                mLastAddedFrameID = ID;
            }
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

        Log.d(LOG_TAG , "Writing Started!");
        while(mIsPlaying){

        }
    }



    public void stopPlaying(){
        mIsPlaying = false;
    }

    public void startSong(long songStart){
        mSongStartTime = songStart;
        double millisTillSongStart =  (songStart + mOffset) - System.currentTimeMillis();
        Log.d(LOG_TAG , "Milliseconds until song start: " + millisTillSongStart);
        mHandler.postDelayed(mStartSong , (long)millisTillSongStart);
    }

    private void startPlaying(){
        Log.d(LOG_TAG , "Write Started");
        mAudioTrack.play();
        mIsPlaying = true;
    }

    public boolean isPlaying(){
        return mIsPlaying;
    }

    public void createAudioTrack(int sampleRate , int channels){
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channels, AudioFormat.ENCODING_PCM_16BIT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
    }

    public void setAudioTrack(AudioTrack track){
        mAudioTrack = track;
    }

    public void release(){
        if(mAudioTrack != null){
            mAudioTrack.flush();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    public void setOffset(double offset){
        mOffset = offset;
    }

    public void setLastFrameID(int lastFrame){
        mLastFrameID = lastFrame;
    }

    public void lastPacket(){
        mLastFrameID = mLastAddedFrameID;
    }
}