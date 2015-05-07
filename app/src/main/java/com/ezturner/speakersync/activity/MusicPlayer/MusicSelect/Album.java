package com.ezturner.speakersync.activity.musicplayer.MusicSelect;

/**
 * Created by Ethan on 2/26/2015.
 */
public class Album implements MusicData{


    private long mID;
    private String mName;
    private String mCoverArt;
    private String mArtist;

    //The class for storing album data
    public Album(long albumID, String albumName , String albumArt , String artist) {
        mID = albumID;
        mName = albumName;
        mCoverArt = albumArt;
        mArtist = artist;
    }

    public long getID() {return mID;}

    public String getPrimaryText() {return mName;}

    public String getArt() {return mCoverArt;}

    //The string that will be below the name
    public String getSecondaryText(){ return "";};
}
