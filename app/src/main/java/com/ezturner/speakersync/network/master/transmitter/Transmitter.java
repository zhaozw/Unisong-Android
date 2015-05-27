package com.ezturner.speakersync.network.master.transmitter;

import com.ezturner.speakersync.audio.AudioFrame;

import java.util.ArrayList;

/**
 * Created by Ethan on 5/22/2015.
 */
public interface Transmitter {

    public void putFrames(ArrayList<AudioFrame> frame);
}
