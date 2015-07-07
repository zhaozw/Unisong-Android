package com.ezturner.speakersync.network.client.receiver;

import com.ezturner.speakersync.audio.AudioFrame;

import java.util.ArrayList;

/**
 * Created by Ethan on 5/22/2015.
 */
public interface Receiver {

    public ArrayList<AudioFrame> getFrames();
}
