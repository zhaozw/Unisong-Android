package com.ezturner.speakersync.network;

/**
 * This is all of the network information on a certain song.
 * Created by Ethan on 8/4/2015.
 */
public class Song {

    private String mName;
    private String mArtist;
    private int mChannels;
    private String mMime;
    //Duration is in milliseconds
    private long mDuration;

    //The # of the song
    private int mSongID;

    public Song(String name , String artist, int channels , String mime, long duration){

    }

    public int getID(){
        return mSongID;
    }

    public String getName(){
        return mName;
    }

    public String getMime(){
        return mMime;
    }

    public int getChannels(){
        return mChannels;
    }

    public long getDuration(){
        return mDuration;
    }
}
