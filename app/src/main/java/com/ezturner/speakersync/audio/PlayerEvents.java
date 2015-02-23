package com.ezturner.speakersync.audio;

/**
 * Created by ezturner on 2/22/2015.
 */
public interface PlayerEvents {
    public void onStart(String mime, int sampleRate,int channels, long duration);
    public void onPlay();
    public void onPlayUpdate(int percent, long currentms, long totalms);
    public void onStop();
    public void onError();
}
