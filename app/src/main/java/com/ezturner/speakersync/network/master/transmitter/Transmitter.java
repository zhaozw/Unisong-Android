package com.ezturner.speakersync.network.master.transmitter;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.AudioObserver;
import com.ezturner.speakersync.audio.master.AACEncoder;

import java.util.List;
import java.util.Map;

/**
 * Created by Ethan on 5/22/2015.
 */
public interface Transmitter extends AudioObserver {

    void setAACEncoder(AACEncoder encoder);
    void setLastFrame(int lastFrame);
    void startSong();
}
