package com.ezturner.speakersync.network;

import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.ntp.SntpClient;

/**
 * Created by Ethan on 5/8/2015.
 */
public class TimeManager {

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
    }

    public void setSongStartTime(long songStartTime){
        mSongStartTime = songStartTime;
    }

    //TODO: make sure that my mSeekTime thing works
    public long getAACPlayTime(int ID){
        return mSongStartTime - mSntpClient.getOffset() + (long)(((1024.0 * ID) / 44100.0) * 1000.0) + mSeekTime;
    }

    public long getPCMDifference(AudioFrame frame){
        return System.currentTimeMillis() - (frame.getPlayTime() - mSeekTime + mSongStartTime - mSntpClient.getOffset());
    }

    public long getOffset(){
        long offset = mSntpClient.getOffset();
        return offset;
    }

    public void seek(long seekTime){
        mSeekTime = seekTime;
    }

}
