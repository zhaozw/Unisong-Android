package io.unisong.android.network.client.receiver;

import io.unisong.android.audio.song.Song;

/**
 * Created by Ethan on 5/22/2015.
 */
public interface Receiver {


    void requestData(Song songToRequest , int startRange , int endRange);

    void destroy();
}
