package io.unisong.android.activity.musicselect;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by Ethan on 2/26/2015.
 */
public class UIPlaylist implements MusicData{

    private final static String LOG_TAG = UIPlaylist.class.getSimpleName();
    private long mID;
    private String mTitle;
    private int mCount;
    private String mData;
    private List<MusicData> mSongs;

    //The class for storing playlist information
    public UIPlaylist(long playlistID, String playlistName, String data) {
        mID = playlistID;
        mTitle = playlistName;
        mData = data;
        mSongs = new ArrayList<>();
        Log.d(LOG_TAG, "mData : " + mData);
    }

    public void addSong(UISong song){
        mSongs.add(song);
    }

    public long getID(){return mID;}

    //TODO: See what we can get from MediaStore for this
    public String getSecondaryText(){return mSongs.size() + " songs";}

    public String getPrimaryText(){return mTitle;}

    public int getType(){
        return PLAYLIST;
    }

    // TODO : see about some sort of playlist imagery?
    public String getImageURL(){return "null";}

    public List<MusicData> getChildren(){
        return mSongs;
    }
}
