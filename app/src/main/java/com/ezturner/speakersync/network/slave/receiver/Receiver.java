package com.ezturner.speakersync.network.slave.receiver;

import com.ezturner.speakersync.audio.AudioFrame;

import java.util.ArrayList;

/**
 * Created by Ethan on 5/22/2015.
 */
public interface Receiver {

    public ArrayList<AudioFrame> getFrames();
}
