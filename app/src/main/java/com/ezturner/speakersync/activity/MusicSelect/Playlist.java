package com.ezturner.speakersync.activity.MusicSelect;

/**
 * Created by Ethan on 2/26/2015.
 */
public class Playlist implements MusicData{

    private long mId;
    private String mTitle;
    private int mCount;

    //The class for storing playlist information
    public Playlist(long playlistID, String playlistName) {
        mId = playlistID;
        mTitle = playlistName;
    }

    public long getId(){return mId;}

    //TODO: See what we can get from MediaStore for this
    public String getSubText(){return "";}

    public String getName(){return mTitle;}

    public int getType(){return 2;}

    public String getArt(){return "";}
}
