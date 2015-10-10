package io.unisong.android.activity.musicselect;

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
    private List<UISong> mSongs;

    //The class for storing album data
    public UIAlbum(long albumID, String albumName, String albumArt, UIArtist artist) {
        mID = albumID;
        mName = albumName;
        mCoverArt = albumArt;
        mArtist = artist.getPrimaryText();
        mSongs = new ArrayList<>();
    }

    public void addSong(UISong song){
        mSongs.add(song);
    }

    public List<UISong> getSongs(){
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
}
