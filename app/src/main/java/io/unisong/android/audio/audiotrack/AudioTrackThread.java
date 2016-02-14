package io.unisong.android.audio.audiotrack;

import android.media.AudioTrack;
import android.util.Log;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.song.Song;
import io.unisong.android.network.ntp.TimeManager;

/**
 * Created by Ethan on 1/31/2016.
 */
public class AudioTrackThread extends Thread {

    private static final String LOG_TAG = AudioTrackThread.class.getSimpleName();

    // Whether we are running and/or waiting
    private boolean running, waiting;

    private AudioTrack audioTrack;
    private Song song;
    private TimeManager timeManager = TimeManager.getInstance();
    private int frameToPlay;
    private long resumeTime;

    public AudioTrackThread(AudioTrack track , Song song, long resumeTime){
        this.audioTrack = track;
        this.song = song;
        this.resumeTime = resumeTime;
    }


    @Override
    public void run(){

        if(!song.started()){
            Log.d(LOG_TAG , "Song has not been started! Starting now.");
            song.start();
            if(resumeTime != 0)
                song.seek(resumeTime);
        }

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        running = true;

        if(resumeTime != 0)
            frameToPlay = song.getFrameIDForTime(resumeTime);

        writeToTrack();
        running = false;
    }

    private void writeToTrack(){


        long waitTime = timeManager.getSongStartTime() + resumeTime - System.currentTimeMillis() ;
        Log.d(LOG_TAG, "Waiting for " + waitTime + "ms, and resume time is :" + resumeTime);
        try{

            if(waitTime > 0) {
                waiting = true;
                synchronized (this) {
                    this.wait(waitTime);
                }
            } else {
                Log.d(LOG_TAG , "Wait time was less than 0!");
            }
        } catch (InterruptedException e){
            Log.d(LOG_TAG , "AudioTrackThread wait interrupted");
        }

        waiting = false;

        // if we've stopped playing, then stop the thread
        if(!running)
            return;



        audioTrack.play();
        if(resumeTime == 0) {
            Log.d(LOG_TAG, "Starting Write, " + (timeManager.getSongStartTime() - System.currentTimeMillis()) + "ms until song start time.");
        } else {
            Log.d(LOG_TAG , "Starting Write, " + (timeManager.getSongStartTime() + resumeTime - System.currentTimeMillis())  + "ms until song start time." );
        }
        long lastWriteTime = System.currentTimeMillis();
        while(running) {
            lastWriteTime = System.currentTimeMillis();

            if(!song.hasPCMFrame(frameToPlay)){
                Log.d(LOG_TAG , "Song does not have frame #" + frameToPlay);
            }


            while (!song.hasPCMFrame(frameToPlay)) {
                try {
                    waiting = true;
                    synchronized (this) {
                        this.wait(1);
                    }
                } catch (InterruptedException e) {

                }
                if (!waiting) {
                    Log.d(LOG_TAG , "We have stopped playing!");
                    return;
                }
                waiting = false;
            }


            AudioFrame frame = song.getPCMFrame(frameToPlay);
            // TODO : synchronize frame playing, and investigate how the time is spent in this thread

            frameToPlay++;

            byte[] data = frame.getData();

            Log.d(LOG_TAG, "Last write time was :" + (System.currentTimeMillis() - lastWriteTime) + "ms ago");
            audioTrack.write(data, 0, data.length);
        }


        Log.d(LOG_TAG, "Write Thread done, last frame #" + frameToPlay);
    }

    public boolean isRunning(){
        return running;
    }

    public void stopPlaying(){
        running = false;

        if(!waiting)
            return;

        synchronized (this) {
            this.interrupt();
        }
    }
}
