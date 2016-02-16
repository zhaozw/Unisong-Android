package io.unisong.android.activity.session.music_select;

import java.util.List;

/**
 * Created by Ethan on 2/26/2015.
 */
public class UISong implements MusicData{

    private long ID;
    private String name;
    private String artistName;
    private String artPath;
    private UIArtist artist;
    private UIAlbum album;
    private String path;
    private long duration;

    private long mArtistID;
    private long mAlbumID;

    //The class for storing song data
    public UISong(long songID, String songTitle, String path, long duration){
        ID = songID;
        name = songTitle;
        this.path = path;
        this.duration = duration;
    }

    public String getName(){
        return name;
    }

    public void setArtist(UIArtist artist){
        this.artist = artist;
        setArtistName(artist.getPrimaryText());
    }

    public void setArtistName(String name){
        artistName = name;
    }

    public void setAlbum(UIAlbum album){
        this.album = album;
        setAlbumArt(album.getImageURL());
    }

    public void setAlbumArt(String path){
        artPath = path;
    }

    public long getID () {return ID;}

    public String getPrimaryText () {return name;}

    //The string that will show up below the name
    public String getSecondaryText() {return artistName;}

    public String getArtist () {return artistName;}

    public String getImageURL(){return artPath;}

    public long getDuration(){
        return duration;
    }

    public int getType(){
        return SONG;
    }

    public String getPath(){
        return path;
    }

    // Not great design lol, but better than making an entire nother interface to separate that functionality from UISong, right?
    public List<MusicData> getChildren(){
        return null;
    }
}
