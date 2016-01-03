package io.unisong.android.activity.musicselect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 10/6/2015.
 */
public class UIGenre implements MusicData {

    private long mID;
    private String mTitle;
    private List<MusicData> mSongs;

    //The class for storing playlist information
    public UIGenre(long playlistID, String playlistName) {
        // TODO : figure out a better way to display genres
        mID = playlistID;
        mTitle = playlistName;
        mSongs = new ArrayList<>();
    }

    public void addSong(UISong song){
        mSongs.add(song);
    }

    public long getID(){return mID;}

    //TODO: See what we can get from MediaStore for this
    public String getSecondaryText(){return mSongs.size() + " songs";}

    public String getPrimaryText(){return mTitle;}

    public String getImageURL(){return "";}

    public int getType(){
        return GENRE;
    }

    public List<MusicData> getChildren(){
        return mSongs;
    }
}