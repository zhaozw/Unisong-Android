package io.unisong.android.network.song;

import android.media.MediaFormat;

import java.util.Map;

import io.unisong.android.activity.musicselect.UISong;
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
    private SongFormat mFormat;
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
    }

    /**
     * This constructor takes in a UISong and creates a LocalSong from it.
     * @param uiSong
     */
    public LocalSong(UISong uiSong){
        // TODO : set duration later.
        super(uiSong.getName() , uiSong.getArtist() , uiSong.getImageURL() );
    }

    public void setFormat(MediaFormat format){
        mFormat = new SongFormat(format);
    }

    @Override
    public long getDuration(){
        // TODO : get duration from
        return 0l;
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

    public boolean hasFrame(int ID){
        return mEncoder.hasFrame(ID);
    }

    public boolean hasPCMFrame(int ID){
        return mDecoder.hasFrame(ID);
    }

    public void seek(long seekTime){
        mDecoder.seek(seekTime);
        mEncoder.seek(seekTime);
    }

    @Override
    public Map<Integer, AudioFrame> getPCMFrames() {
        return mDecoder.getFrames();
    }

    @Override
    public SongFormat getFormat() {
        return mFormat;
    }

    @Override
    public void addFrame(AudioFrame frame) {

    }

}
