package io.unisong.android.network.song;

import org.json.JSONObject;

import java.util.Map;

import io.unisong.android.audio.AudioFrame;

/**
 * This is all of the network information on a certain song.
 * Created by Ethan on 8/4/2015.
 */
public abstract class Song {
    
    protected String mName;
    protected String mArtist;
    protected boolean mStarted;

    protected String mImageURL;

    //The # of the song
    protected int mSongID;

    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     * @param name
     * @param artist
     * @param imageURL
     */
    public Song(String name , String artist, int ID , String imageURL){
        mName = name;
        mArtist = artist;
        mImageURL = imageURL;
        mSongID = ID;
        mStarted = false;
    }

    public Song(String name , String artist, String imageURL){
        mName = name;
        mArtist = artist;
        mImageURL = imageURL;
        mStarted = false;
    }

    public int getID(){
        return mSongID;
    }

    public String getName(){
        return mName;
    }

    public String getArtist(){return mArtist;}

    public abstract String getImageURL();

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

    public abstract long getDuration();

    public abstract JSONObject toJSON();

    public abstract void update(JSONObject songJSON);

    public boolean started(){
        return mStarted;
    }
}