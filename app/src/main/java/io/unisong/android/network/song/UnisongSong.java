package io.unisong.android.network.song;

import java.util.Map;

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
        super(name, artist,ID ,  imageURL);
        mSongDecoder = new SongDecoder(inputFormat);
    }

    // TODO : write and actually implement.
    public long getDuration(){
        return 0l;
    }
    @Override
    public AudioFrame getFrame(int ID) {
        return mSongDecoder.getFrame(ID);
    }

    @Override
    public AudioFrame getPCMFrame(int ID) {
        return mSongDecoder.getFrame(ID);
    }

    @Override
    public boolean hasFrame(int ID) {
        return mSongDecoder.hasInputFrame(ID);
    }

    @Override
    public boolean hasPCMFrame(int ID) {
        return false;
    }

    /**
     * Starts the decoding of the song.
     */
    @Override
    public void start() {
        mSongDecoder.decode(0);
    }

    @Override
    public void seek(long seekTime) {

    }

    @Override
    public Map<Integer, AudioFrame> getPCMFrames() {
        return null;
    }

    @Override
    public SongFormat getFormat() {
        return null;
    }

    @Override
    public void addFrame(AudioFrame frame) {

    }

    /**
     * This provides a received Unisong frame from a network source.
     * @param frame
     */
    public void addInputFrame(AudioFrame frame){
        mSongDecoder.addInputFrame(frame);
    }


}
