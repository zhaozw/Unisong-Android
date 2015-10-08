package io.unisong.android.activity.musicselect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 10/6/2015.
 */
public class UIGenre implements MusicData {

    private long mID;
    private String mTitle;
    private List<UIAlbum> mAlbums;

    //The class for storing playlist information
    public UIGenre(long playlistID, String playlistName) {
        mID = playlistID;
        mTitle = playlistName;
        mAlbums = new ArrayList<>();
    }

    public void addAlbum(UIAlbum album){
        mAlbums.add(album);
    }

    public long getID(){return mID;}

    //TODO: See what we can get from MediaStore for this
    public String getSecondaryText(){return "Genre placeholder";}

    public String getPrimaryText(){return mTitle;}

    public String getImageURL(){return "";}

    public int getType(){
        return GENRE;
    }
}