package io.unisong.android.audio;

import java.util.Map;

/**
 * Created by Ethan on 8/4/2015.
 */
public interface Decoder {


    AudioFrame getFrame(int ID);

    void seek(long seekTime);

    boolean hasFrame(int ID);

    void destroy();

    Map<Integer, AudioFrame> getFrames();

    void addInputFrame(AudioFrame frame);
}
