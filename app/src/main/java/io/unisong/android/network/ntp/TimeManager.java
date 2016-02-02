package io.unisong.android.network.ntp;

import android.util.Log;

import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.song.Song;
import io.unisong.android.network.session.UnisongSession;

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
    private AudioStatePublisher publisher;

    // songStartTime configured with nanoTime();
    private long mNanoSongStartTime;

    public TimeManager(){
        sntpClient = new SntpClient();
        publisher = AudioStatePublisher.getInstance();
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

    /**
     * Calculates the progress for the SeekBar.
     * @return
     */
    public int getProgress(){
        int progress = 100;

        if(publisher.getState() == AudioStatePublisher.IDLE)
            return progress = 100;


        Song song = UnisongSession.getCurrentSession().getCurrentSong();
        if(song == null) {
            Log.d(LOG_TAG , "CurrentSong is null!");
            UnisongSession.getCurrentSession().getUpdate();
            return 100;
        }

        long timePlayed = 0;

        if(publisher.getState() == AudioStatePublisher.PLAYING){
            timePlayed = System.currentTimeMillis() - getSongStartTime();
        } else if(publisher.getState() == AudioStatePublisher.PAUSED){
            timePlayed = publisher.getPausedTime();
        }


        double fraction = (double)(timePlayed) / (double) (song.getDuration() / 1000);
        progress = (int) (fraction * 100);

        if(progress < 0)
            return 0;

        if(progress > 100)
            return 100;

        return progress;
    }

    @Override
    public void update(int state) {
        //TODO : update songStartTime on resume
    }

    public void destroy(){
        publisher = null;
        if(sntpClient != null) {
            sntpClient.destroy();
            sntpClient = null;
        }

    }
}
