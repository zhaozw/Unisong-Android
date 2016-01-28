package io.unisong.android.network.ntp;

import io.unisong.android.audio.AudioObserver;

/**
 * Created by Ethan on 5/8/2015.
 */
public class TimeManager implements AudioObserver {

    private static TimeManager instance;
    public static TimeManager getInstance(){
        return instance;
    }

    private static final String LOG_TAG = TimeManager.class.getSimpleName();
    //The time offset returned by the SntpClient class
    private SntpClient sntpClient;

    //The seek time adjustment
    private long seekTime;

    // The time that the current song starts at, with the offset applied
    // To convert to this device's local time, subtract sntpClient.getOffset().
    private long songStartTime;

    // songStartTime configured with nanoTime();
    private long mNanoSongStartTime;

    public TimeManager(){
        sntpClient = new SntpClient();
        // TODO : listen for clock change events
        seekTime = 0;
        instance = this;
    }

    public void setSongStartTime(long songStartTime){
        this.songStartTime = songStartTime;
    }

    //TODO: make sure that my seekTime thing works
    public long getAACPlayTime(int ID){
        return songStartTime - sntpClient.getOffset() + (long)(((1024.0 * ID) / 44100.0) * 1000.0);
    }

    public long getPCMDifference(long playTime){
        return System.currentTimeMillis() - (songStartTime + playTime - sntpClient.getOffset());
    }

    public long getOffset(){
        return sntpClient.getOffset();
    }

    public long getSongStartTime(){
        return songStartTime;
    }

    @Override
    public void update(int state) {
        //TODO : update songStartTime on resume
    }
}
