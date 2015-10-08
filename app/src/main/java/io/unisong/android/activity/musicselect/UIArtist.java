package io.unisong.android.activity.musicselect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 2/26/2015.
 */
public class UIArtist implements MusicData{

    private long mID;
    private String mName;
    private String mNumAlbums;
    private String mTracks;
    private List<UIAlbum> mAlbums;

    //The class for storing artist data
    public UIArtist(long artistID, String artistName) {
        mID = artistID;
        mName = artistName;
        mAlbums = new ArrayList<>();
    }

    public void addAlbum(UIAlbum album){
        mAlbums.add(album);
    }

    public long getID(){return mID;}

    public String getPrimaryText(){return mName;}

    //The string that will be below the name
    public String getSecondaryText(){ return mNumAlbums + " album, " + mTracks + " songs";};

    public String getImageURL(){return "";}

    public int getType(){
        return ARTIST;
    }

}
