package io.unisong.android.network.song;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.client.SongDecoder;

/**
 * This class handles songs that are broadcasted over the Unisong network.
 * Created by Ethan on 10/3/2015.
 */
public class UnisongSong extends Song {

    private SongDecoder mSongDecoder;

    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     *
     * @param name
     * @param artist
     * @param duration
     * @param imageURL
     */
    public UnisongSong(String name, String artist, long duration,int ID ,  String imageURL, SongFormat inputFormat) {
        super(name, artist, duration,ID ,  imageURL);
        mSongDecoder = new SongDecoder(inputFormat);
    }

    @Override
    public AudioFrame getPCMFrame(int ID) {
        return mSongDecoder.getFrame(ID);
    }

    /**
     * Starts the decoding of the song.
     */
    @Override
    public void start() {
        mSongDecoder.decode(0);
    }

    /**
     * This provides a received Unisong frame from a network source.
     * @param frame
     */
    public void addInputFrame(AudioFrame frame){
        mSongDecoder.addInputFrame(frame);
    }


}
