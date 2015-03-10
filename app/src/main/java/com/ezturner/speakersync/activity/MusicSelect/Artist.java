package com.ezturner.speakersync.activity.MusicSelect;

/**
 * Created by Ethan on 2/26/2015.
 */
public class Artist implements MusicData{

    private long mID;
    private String mName;

    //The class for storing artist data
    public Artist(long artistID, String artistName) {
        mID = artistID;
        mName = artistName;
    }

    public long getID(){return mID;}

    public String getName(){return mName;}

    //The string that will be below the name
    public String getSubText(){ return "";};

    public String getArt(){return "";}

}
