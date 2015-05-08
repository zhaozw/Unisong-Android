package com.ezturner.speakersync.activity.MusicPlayer.MusicSelect;

/**
 * Created by Ethan on 2/26/2015.
 */
public class Playlist implements MusicData{

    private long mID;
    private String mTitle;
    private int mCount;

    //The class for storing playlist information
    public Playlist(long playlistID, String playlistName) {
        mID = playlistID;
        mTitle = playlistName;
    }

    public long getID(){return mID;}

    //TODO: See what we can get from MediaStore for this
    public String getSecondaryText(){return "";}

    public String getPrimaryText(){return mTitle;}

    public int getType(){return 2;}

    public String getArt(){return "";}
}
