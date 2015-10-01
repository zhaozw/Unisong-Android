package io.unisong.android.network.host.transmitter;

import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioSource;

/**
 * Created by Ethan on 5/22/2015.
 */
public interface Transmitter extends AudioObserver {

    void setAudioSource(AudioSource source);
    void setLastFrame(int lastFrame);
    void startSong(long songStartTime, int channels, int songID);
}
