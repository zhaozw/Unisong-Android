package io.unisong.android.activity.session.musicselect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 2/26/2015.
 */
public class UIArtist implements MusicData{

    private long mID;
    private String mName;
    private List<MusicData> mAlbums;

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
    public String getSecondaryText(){
        return mAlbums.size() + " albums, " + getNumSongs() + " songs";
    }

    public int getNumSongs(){
        int songs = 0;
        for(MusicData album : mAlbums){
            songs += ((UIAlbum)album).getSongs().size();
        }
        return songs;
    }

    public String getImageURL(){return "";}

    public int getType(){
        return ARTIST;
    }

    @Override
    public List<MusicData> getChildren() {
        return mAlbums;
    }

}
