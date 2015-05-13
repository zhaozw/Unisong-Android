package com.ezturner.speakersync.network;

import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;

/**
 * Created by Ethan on 5/8/2015.
 */
public class TimeManager {

    private static final String LOG_TAG = TimeManager.class.getSimpleName();
    //The time offset returned by the SntpClient class
    private long mOffset;

    //The seek time adjustment
    private long mSeekTime;

    // The time that the current song starts at, with the offset applied
    // To convert to this device's local time, subtract mOffset.
    private long mSongStartTime;

    public TimeManager(){
        mOffset = 0;
    }


    public void setOffset(double offset){
        Log.d(LOG_TAG, "Offset Received: " + offset);
        mOffset = (long)Math.round(offset);
    }

    public void setSongStartTime(long songStartTime){
        mSongStartTime = songStartTime;
    }

    //TODO: make sure that my mSeekTime thing works
    public long getAACPlayTime(int ID){
        return mSongStartTime - mOffset + (long)(((1024.0 * ID) / 44100.0) * 1000.0) + mSeekTime;
    }

    public long getPCMDifference(AudioFrame frame){
        return System.currentTimeMillis() - (frame.getPlayTime() - mSeekTime + mSongStartTime - mOffset);
    }

    public long getOffset(){
        return mOffset;
    }

    public void seek(long seekTime){
        mSeekTime = seekTime;
    }

}
