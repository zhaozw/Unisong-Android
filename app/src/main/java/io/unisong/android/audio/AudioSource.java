package io.unisong.android.audio;

import java.util.Map;

/**
 * This is the standard interface for all original audio sources,
 * whether it be SoundCloud, Spotify, a local file, pandora, etc.
 *
 * Created by ezturner on 8/10/2015.
 */
public interface AudioSource {

    // Retrieve a given AudioFrame
    AudioFrame getFrame(int ID);

    // Get the entire map of AudioFrames - perhaps not necessary.
    Map<Integer, AudioFrame> getFrames();

    // Discover whether a given AudioFrame is currently held by the Source.
    boolean hasFrame(int ID);

    // Tells the source that a given frame has been used, so that we may get more frames.
    // This is used so that we can support songs of any length without massive memory useage
    // TODO : consider caching to disk.
    void frameUsed(int ID);


}
