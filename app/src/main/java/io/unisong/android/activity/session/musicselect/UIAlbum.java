package io.unisong.android.activity.session.musicselect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 2/26/2015.
 */
public class UIAlbum implements MusicData{


    private long mID;
    private String mName;
    private String mCoverArt;
    private String mArtist;
    private List<MusicData> mSongs;

    //The class for storing album data
    public UIAlbum(long albumID, String albumName, String albumArt, String artistName) {
        mID = albumID;
        mName = albumName;
        mCoverArt = albumArt;
        mArtist = artistName;
        mSongs = new ArrayList<>();
    }

    public void addSong(UISong song){
        mSongs.add(song);
    }

    public List<MusicData> getSongs(){
        return mSongs;
    }

    public long getID() {return mID;}

    public String getPrimaryText() {return mName;}

    public String getImageURL() {return mCoverArt;}

    @Override
    public int getType() {
        return ALBUM;
    }

    //The string that will be below the name
    public String getSecondaryText(){
        if(mArtist == null)
            return "<unknown>";

        return mArtist;
    }

    public List<MusicData> getChildren(){
        return mSongs;
    }
}