package io.unisong.android.audio;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.unisong.android.Unisong;
import io.unisong.android.audio.audiotrack.AudioTrackManager;
import io.unisong.android.audio.song.Song;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.host.Broadcaster;
import io.unisong.android.network.ntp.TimeManager;
import io.unisong.android.network.session.UnisongSession;

/**
 * An Implementation of the Observer/Subject design pattern, this is the publisher
 * that will broadcast state updates about the application to its subscribers
 *
 * In practice this class is a mix between a publisher/subscriber class and facade.
 * Whenever an operation is to be performed regarding the audio state, this will allow for
 * a simple way to both propagate state changes and provide a simple interface to provide all
 * relevant information to the variety of classes that will need it, without having to modify large interfaces
 * every time an architecture change is made.
 *
 * This class will handle the Audio State (playing, pausing, skipping, resuming, new song)
 *
 * To use this class, first update any relevant information with the setter methods and then call update()
 * Created by ezturner on 5/27/2015.
 */
public class AudioStatePublisher {

    private static final String LOG_TAG = AudioStatePublisher.class.getSimpleName();

    private static AudioStatePublisher instance;

    public static final int IDLE = 0;
    public static final int PAUSED = 1;
    public static final int SEEK = 2;
    public static final int PLAYING = 3;
    public static final int RESUME = 4;
    public static final int END_SONG = 5;
    public static final int START_SONG = 6;

    private Broadcaster broadcaster;
    //The time that we are seeking to
    private long seekTime;

    //The time that the song will be resumed at
    private long resumeTime;

    private long pausedTime;
    private int songToEnd;
    private int state;
    private Handler handler;

    //The boolean used to tell if a value has been updated (for pause time)
    private boolean updated;
    private List<AudioObserver> observers;

    private AudioTrackManager manager;

    //Sets the state for Idle and instantiates the Observers arraylist
    public AudioStatePublisher(){
        handler = new Handler();
        state = IDLE;
        observers = new ArrayList<>();
        instance = this;
    }

    public static AudioStatePublisher getInstance(){
        return instance;
    }

    //Attaches a AudioObserver to this AudioStateSubject
    public void attach(AudioObserver observer){
        synchronized (observers) {
            if (!observers.contains(observer))
                observers.add(observer);
        }
    }

    public void detach(AudioObserver observer){
        synchronized (observers) {
            observers.remove(observer);
        }
    }

    public void update(int state){
        try {
            synchronized (observers) {
                Song song = null;

                //Set the state
                if (state == RESUME || state == START_SONG) {
                    this.state = PLAYING;
                } else if (state == END_SONG) {
                    this.state = IDLE;
                    song = UnisongSession.getCurrentSession().getCurrentSong();
                } else if (state != SEEK) {
                    this.state = state;
                }

                if (state == PAUSED && resumeTime != 0) {
                    // If I put this into the constructor it causes a loop of instantiation between
                    // AudioTrackManager and this class due to their dual singleton design pattern.
                    if (manager == null) {
                        manager = AudioTrackManager.getInstance();
                    }
                    resumeTime = manager.getLastFrameTime();
                }

                // set the songStartTime first
                if (broadcaster != null) {
                    broadcaster.update(state);
                    observers.remove(observers.indexOf(broadcaster));
                }

                for (int i = 0; i < observers.size(); i++) {
                    observers.get(i).update(state);
                }

                if (broadcaster != null)
                    observers.add(broadcaster);

                if (song != null) {
                    if (observers.contains(song)) {
                        observers.remove(song);
                        Song currentSong = UnisongSession.getCurrentSession().getCurrentSong();
                        if (currentSong != null)
                            observers.add(song);
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setState(int state){
        this.state = state;
    }

    public long getResumeTime(){
        return resumeTime;
    }

    public long getSeekTime(){
        return seekTime;
    }

    public long getPausedTime(){
        return pausedTime;
    }

    public int getState(){return state;}

    /**
     * The Seek method. It updates the seek time and then executes an update()
     */
    public void seek(long time){
        // TODO : if we are playing, pause then resume
        int previousState = state;
        if(previousState == PLAYING) {
            update(AudioStatePublisher.PAUSED);
            pausedTime = time;
        }

        if(previousState == PAUSED){
            pausedTime = time;
        }

        seekTime = time;
        resumeTime = time;
        update(AudioStatePublisher.SEEK);
        if(previousState == PLAYING)
            handler.postDelayed(() -> {
                resume(resumeTime);
            }, 5);
    }

    /**
     * The Resume method, which updates the resume time and then executes an update()
     */
    public void resume(long resumeTime){
        this.resumeTime = resumeTime;
        TimeManager timeManager = TimeManager.getInstance();
        long newStartTime = System.currentTimeMillis() - resumeTime + CONSTANTS.RESUME_DELAY;
        timeManager.setSongStartTime(newStartTime);
        Log.d(LOG_TAG , "Setting new songStartTime to : " + new Date(newStartTime).toString());
        update(AudioStatePublisher.RESUME);
    }

    public void pause(){
        Log.d(LOG_TAG, "Pausing");
        pausedTime = System.currentTimeMillis() - TimeManager.getInstance().getSongStartTime();
        update(AudioStatePublisher.PAUSED);
    }

    public void startSong(){
        resumeTime = 0;
        update(AudioStatePublisher.START_SONG);
//        update(AudioStatePublisher.PLAYING);
    }

    public void play(){
        resumeTime = 0;
        update(AudioStatePublisher.PLAYING);
    }

    /**
     * Ends the song, notifying all AudioObservers of this event
     * Then, if we have a song queued to be played, play it.
     * @param songID
     */
    public void endSong(int songID){
        resumeTime = 0;
        seekTime = 0;
        pausedTime = 0;
        songToEnd = songID;
        Log.d(LOG_TAG, "Song Ending!");
        update(AudioStatePublisher.END_SONG);
        if(UnisongSession.getCurrentSession().getCurrentSong() != null)
            update(AudioStatePublisher.START_SONG);
    }

    public int getSongToEnd(){
        return songToEnd;
    }

    public void clear(){
        resumeTime = 0;
        seekTime = 0;
        pausedTime = 0;
        broadcaster = null;
    }

    /**
     * This method is so that the songStartTime can be set before notifying all of the other
     * AudioSubscribers
     */
    public void setBroadcaster(Broadcaster broadcaster){
        this.broadcaster = broadcaster;
    }


    public void destroy(){
        observers = new ArrayList<>();
        manager = null;
        broadcaster = null;
    }
}