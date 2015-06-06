package com.ezturner.speakersync.network;

import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.ntp.SntpClient;

/**
 * Created by Ethan on 5/8/2015.
 */
public class TimeManager {

    private static TimeManager sIntance;
    public static TimeManager getInstance(){
        return sIntance;
    }

    private static final String LOG_TAG = TimeManager.class.getSimpleName();
    //The time offset returned by the SntpClient class
    private SntpClient mSntpClient;

    //The seek time adjustment
    private long mSeekTime;

    // The time that the current song starts at, with the offset applied
    // To convert to this device's local time, subtract mSntpClient.getOffset().
    private long mSongStartTime;

    public TimeManager(SntpClient client){
        mSntpClient = client;
        mSeekTime = 0;
        sIntance = this;
    }

    public void setSongStartTime(long songStartTime){
        mSongStartTime = songStartTime;
    }

    //TODO: make sure that my mSeekTime thing works
    public long getAACPlayTime(int ID){
        return mSongStartTime - mSntpClient.getOffset() + (long)(((1024.0 * ID) / 44100.0) * 1000.0);
    }

    public long getPCMDifference(long playTime){
        return System.currentTimeMillis() - (mSongStartTime + playTime - mSntpClient.getOffset());
    }

    public long getOffset(){
        long offset = mSntpClient.getOffset();
        return offset;
    }

    public long getSongStartTime(){
        return mSongStartTime;
    }
}
