package io.unisong.android.activity.MusicPlayer.MusicSelect;

/**
 * Created by Ethan on 2/26/2015.
 */
public class Artist implements MusicData{

    private long mID;
    private String mName;
    private String mAlbums;
    private String mTracks;

    //The class for storing artist data
    public Artist(long artistID, String artistName, String albums, String tracks) {
        mID = artistID;
        mName = artistName;
        mAlbums = albums;
        mTracks = tracks;
    }

    public long getID(){return mID;}

    public String getPrimaryText(){return mName;}

    //The string that will be below the name
    public String getSecondaryText(){ return mAlbums + " album, " + mTracks + " songs";};

    public String getArt(){return "";}

}
