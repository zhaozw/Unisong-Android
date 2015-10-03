package io.unisong.android.network.song;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.master.AACEncoder;
import io.unisong.android.audio.master.FileDecoder;

/**
 * Created by Ethan on 10/3/2015.
 */
public class LocalSong extends Song {

    private String mPath;
    private FileDecoder mDecoder;
    private AACEncoder mEncoder;
    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     *
     * @param name
     * @param artist
     * @param duration
     * @param imageURL
     */
    public LocalSong(String name, String artist, long duration,int ID , String imageURL, String path) {
        super(name, artist, duration, ID, imageURL);
        mPath = path;
        mDecoder = new FileDecoder(mPath);
        mEncoder = new AACEncoder();
    }

    /**
     * Returns an encoded frame.
     * @param ID
     * @return
     */
    public AudioFrame getFrame(int ID) {
        return mEncoder.getFrame(ID);
    }

    /**
     * Returns the PCM frame with the specified ID
     * @param ID - The ID of the given frame
     * @return
     */
    @Override
    public AudioFrame getPCMFrame(int ID) {
        return mDecoder.getFrame(ID);
    }

    /**
     * Begins the PCM decoding and AAC encoding.
     */
    public void start(){
        mDecoder.startDecode();
        mEncoder.encode(0 , super.getID() , mPath);
    }
}
