package io.unisong.android.activity.session.music_select;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 2/26/2015.
 */
public class UIAlbum implements MusicData{


    private long ID;
    private String name;
    private String coverArt;
    private String artist;
    private List<MusicData> songs;

    //The class for storing album data
    public UIAlbum(long albumID, String albumName, String albumArt, String artistName) {
        ID = albumID;
        name = albumName;
        coverArt = albumArt;
        artist = artistName;
        songs = new ArrayList<>();
    }

    public void addSong(UISong song){
        songs.add(song);
    }

    public List<MusicData> getSongs(){
        return songs;
    }

    public long getID() {return ID;}

    public String getPrimaryText() {return name;}

    public String getImageURL() {return coverArt;}

    @Override
    public int getType() {
        return ALBUM;
    }

    //The string that will be below the name
    public String getSecondaryText(){
        if(artist == null)
            return "<unknown>";

        return artist;
    }

    public List<MusicData> getChildren(){
        return songs;
    }
}
