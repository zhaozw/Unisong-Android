package io.unisong.android.network.client.receiver;

import io.unisong.android.audio.AudioFrame;

import java.util.ArrayList;

/**
 * Created by Ethan on 5/22/2015.
 */
public interface Receiver {

    public ArrayList<AudioFrame> getFrames();
}
