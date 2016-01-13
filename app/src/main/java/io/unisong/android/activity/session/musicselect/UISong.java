package io.unisong.android.activity.session.musicselect;

import java.util.List;

/**
 * Created by Ethan on 2/26/2015.
 */
public class UISong implements MusicData{

    private long mID;
    private String mName;
    private String mArtistName;
    private String mArtPath;
    private UIArtist mArtist;
    private UIAlbum mAlbum;
    private String mPath;
    private long mDuration;

    private long mArtistID;
    private long mAlbumID;

    //The class for storing song data
    public UISong(long songID, String songTitle, String path, long duration){
        mID = songID;
        mName = songTitle;
        mPath = path;
        mDuration = duration;
    }

    public String getName(){
        return mName;
    }

    public void setArtist(UIArtist artist){
        mArtist = artist;
        setArtistName(artist.getPrimaryText());
    }

    public void setArtistName(String name){
        mArtistName = name;
    }

    public void setAlbum(UIAlbum album){
        mAlbum = album;
        setAlbumArt(album.getImageURL());
    }

    public void setAlbumArt(String path){
        mArtPath = path;
    }

    public long getID () {return mID;}

    public String getPrimaryText () {return mName;}

    //The string that will show up below the name
    public String getSecondaryText() {return mArtistName;}

    public String getArtist () {return mArtistName;}

    public String getImageURL(){return mArtPath;}

    public long getDuration(){
        return mDuration;
    }

    public int getType(){
        return SONG;
    }

    public String getPath(){
        return mPath;
    }

    // Not great design lol, but better than making an entire nother interface to separate that functionality from UISong, right?
    public List<MusicData> getChildren(){
        return null;
    }
}
