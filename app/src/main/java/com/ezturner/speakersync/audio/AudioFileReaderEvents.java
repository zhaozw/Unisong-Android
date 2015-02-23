package com.ezturner.speakersync.audio;

import android.util.Log;

/**
 * Created by ezturner on 2/22/2015.
 */
public class AudioFileReaderEvents implements PlayerEvents{

    private static final String LOG_TAG = "AudioFileReaderEvent";

    public AudioFileReaderEvents(){

    }

    @Override
    public void onStart(String mime, int sampleRate, int channels, long duration) {
        Log.d(LOG_TAG , "Mime: " + mime + " , Sample Rate : " + sampleRate + " , Channels : " + channels + " , Duration: " + duration);
    }

    @Override
    public void onPlay() {
        Log.d(LOG_TAG , "Audio playing!");
    }

    @Override
    public void onPlayUpdate(int percent, long currentms, long totalms) {
        Log.d(LOG_TAG, "Percent: " + percent + "% , Current MS: " +currentms + " , Total MS: " + totalms);
    }

    @Override
    public void onStop() {
        Log.d(LOG_TAG , "Audio Stopped");
    }

    @Override
    public void onError() {
        Log.d(LOG_TAG , "There has been an error!");
    }
}
