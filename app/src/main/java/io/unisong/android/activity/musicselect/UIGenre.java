package io.unisong.android.activity.musicselect;

/**
 * Created by Ethan on 10/6/2015.
 */
public class UIGenre implements MusicData {

    private long mID;
    private String mTitle;
    private int mCount;

    //The class for storing playlist information
    public UIGenre(long playlistID, String playlistName, int count) {
        mID = playlistID;
        mTitle = playlistName;
        mCount = count;
    }

    public long getID(){return mID;}

    //TODO: See what we can get from MediaStore for this
    public String getSecondaryText(){return mCount + " songs";}

    public String getPrimaryText(){return mTitle;}

    public int getType(){return 2;}

    public String getImageURL(){return "";}
}