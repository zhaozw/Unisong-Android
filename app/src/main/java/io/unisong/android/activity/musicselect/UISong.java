package io.unisong.android.activity.musicselect;

/**
 * Created by Ethan on 2/26/2015.
 */
public class UISong implements MusicData{

    private long mID;
    private String mName;
    private String mArtist;
    private String mArt;
    private String mData;

    //The class for storing song data
    public UISong(long songID, String songTitle, String songArtist, String albumart){
        mID = songID;
        mName = songTitle;
        mArtist = songArtist;
        mArt = albumart;
    }

    public long getID () {return mID;}

    public String getPrimaryText () {return mName;}

    //The string that will show up below the name
    public String getSecondaryText() {return mArtist;}

    public String getArtist () {return mArtist;}

    public String getImageURL(){return mArt;}

    public int getType(){return 3;}
}
