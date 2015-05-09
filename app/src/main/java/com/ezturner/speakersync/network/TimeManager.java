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

    public long getAACPlayTime(int ID){
        return mSongStartTime - mOffset + (long)(((1024.0 * ID) / 44100.0) * 1000.0);
    }

    public long getPCMPlayTime(long playTime){
        return playTime + mSongStartTime - mOffset;
    }

    public long getPCMDifference(AudioFrame frame){
        return System.currentTimeMillis() - (frame.getPlayTime() + mSongStartTime - mOffset);
    }

    public long getOffset(){
        return mOffset;
    }

}
