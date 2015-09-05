package io.unisong.android.network.master.transmitter;

import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.master.AACEncoder;

/**
 * Created by Ethan on 5/22/2015.
 */
public interface Transmitter extends AudioObserver {

    void setAACEncoder(AACEncoder encoder);
    void setLastFrame(int lastFrame);
    void startSong();
}
