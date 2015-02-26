package com.ezturner.speakersync.activity.MusicSelect;

/**
 * Created by Ethan on 2/26/2015.
 */
public class Album implements MusicData{


    private long mId;
    private String mName;
    private String mCoverArt;
    private String mArtist;

    //The class for storing artist data
    public Album(long albumID, String albumName , String albumArt , String artist) {
        mId = albumID;
        mName = albumName;
        mCoverArt = albumArt;
        mArtist = artist;
    }

    public long getId() {return mId;}

    public String getName() {return mName;}

    public String getArt() {return mCoverArt;}

    //The string that will be below the name
    public String getSubText(){ return "";};
}
