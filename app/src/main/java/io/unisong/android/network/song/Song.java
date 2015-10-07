package io.unisong.android.network.song;

import java.util.List;
import java.util.Map;

import io.unisong.android.audio.AudioFrame;

/**
 * This is all of the network information on a certain song.
 * Created by Ethan on 8/4/2015.
 */
public abstract class Song {
    
    private String mName;
    private String mArtist;
    //Duration is in milliseconds
    private long mDuration;
    private int mType;

    private String mImageURL;

    //The # of the song
    private int mSongID;

    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     * @param name
     * @param artist
     * @param duration
     * @param imageURL
     */
    public Song(String name , String artist, long duration, int ID , String imageURL){
        mName = name;
        mArtist = artist;
        mDuration = duration;
        mImageURL = imageURL;
    }

    public int getID(){
        return mSongID;
    }

    public String getName(){
        return mName;
    }

    public String getArtist(){return mArtist;}

    public long getDuration(){
        return mDuration;
    }

    public String getImageURL(){
        return mImageURL;
    }

    /**
     * Returns the encoded frame with the specified ID
     * @param ID
     * @return
     */
    public abstract AudioFrame getFrame(int ID);

    /**
     * Returns the raw PCM frame with a given ID
     * @param ID
     * @return
     */
    public abstract AudioFrame getPCMFrame(int ID);

    public abstract boolean hasFrame(int ID);

    public abstract boolean hasPCMFrame(int ID);

    public abstract void start();

    public abstract void seek(long seekTime);

    public abstract Map<Integer, AudioFrame> getPCMFrames();

    public abstract SongFormat getFormat();

    public abstract void addFrame(AudioFrame frame);

}
