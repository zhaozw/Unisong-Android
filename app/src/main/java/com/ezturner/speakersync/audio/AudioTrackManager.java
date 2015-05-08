package com.ezturner.speakersync.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
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
                    //TODO: uncomment these after it's safe
                    startPlaying();
                }
        }
    };

    private void writeToTrack(){
        while(isPlaying()) {
            if (mFrameToPlay == mLastFrameID) {
                mIsPlaying = false;
                return;
            }



            AudioFrame frame;
            synchronized (mFrames) {
                frame = mFrames.get(mFrameToPlay);
            }

            if (frame == null) {
                Log.d(LOG_TAG, "Frame ID is: " + mFrameToPlay + " , waiting");
                synchronized (this) {
                    while (!mFrames.containsKey(mFrameToPlay)) {
                        try {
                            this.wait(1);
                        } catch (InterruptedException e) {

                        }
                    }
                }
                frame = mFrames.get(mFrameToPlay);
            }
            mFrameToPlay++;


            long difference = System.currentTimeMillis() - (frame.getPlayTime() + mSongStartTime - (long) mOffset);

//            Log.d(LOG_TAG, "Current time is : " + System.currentTimeMillis() + " and play time is : " + frame.getPlayTime() + " and Song Start time is : " + mSongStartTime);



            if(difference <= -30){
                long before = System.currentTimeMillis();
                synchronized (this) {
                    try {
                        this.wait(Math.abs(difference));
                    } catch (InterruptedException e) {

                    }
                }
            } else {
                int index = mFrameToPlay;

                while (difference >= 30) {
                    AudioFrame nextFrame = null;
                    synchronized (mFrames) {
                        nextFrame = mFrames.get(index);
                    }
                    if (nextFrame != null) {
                        difference = System.currentTimeMillis() - (nextFrame.getPlayTime() + mSongStartTime - (long) mOffset);
                        index++;
//                        Log.d(LOG_TAG , "Difference v2 is : " + difference);
                    } else if(nextFrame == null){
                        //TODO: handle
                    }
                }
                mFrameToPlay = index;
            }


            //TODO: handle it when this is null AND when the stream is over
            byte[] data = frame.getData();

            mTest++;

            if (mTest >= 100) {
                mTest = 0;
                difference = System.currentTimeMillis() - (frame.getPlayTime() + mSongStartTime - (long) mOffset);
                Log.d(LOG_TAG, "Time difference is : " + difference);
            }
            mAudioTrack.write(data, 0, data.length);
            synchronized (mFrames) {
                mFrames.remove(frame.getID());
            }
        }
    }
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
        synchronized (mFrames) {
            return mFrames.get(ID);
        }
    }

    private Thread getWriteThread(){
        return new Thread(new Runnable()  {
            public void run() {
                writeToTrack();
            }
        });
    }

    private int mTest = 0;

    public void stopPlaying(){
        mIsPlaying = false;
    }

    public void startSong(long songStart){
        mSongStartTime = songStart;
        double millisTillSongStart =  (songStart - mOffset) - System.currentTimeMillis();
        Log.d(LOG_TAG , "Milliseconds until song start: " + millisTillSongStart + " and mOffset is :" + mOffset);
        mHandler.postDelayed(mStartSong, (long) millisTillSongStart);
    }

    private void startPlaying(){
        Log.d(LOG_TAG, "Write Started, difference is: " + (System.currentTimeMillis() - (mSongStartTime - mOffset))+ " mOffset is : " + mOffset);
        if(mAudioTrack == null){
            createAudioTrack(44100 , 2);
        }
        mAudioTrack.play();
        mIsPlaying = true;
        mWriteThread = getWriteThread();
        mWriteThread.start();
    }

    public boolean isPlaying(){
        return mIsPlaying;
    }

    public void createAudioTrack(int sampleRate , int channels){
        Log.d(LOG_TAG , "Creating Audio Track sampleRate : " + sampleRate + " and channels " + channels);
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