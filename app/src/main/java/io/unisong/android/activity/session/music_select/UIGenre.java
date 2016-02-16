package io.unisong.android.activity.session.music_select;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 10/6/2015.
 */
public class UIGenre implements MusicData {

    private long ID;
    private String title;
    private List<MusicData> songs;

    //The class for storing playlist information
    public UIGenre(long playlistID, String playlistName) {
        // TODO : figure out a better way to display genres
        ID = playlistID;
        title = playlistName;
        songs = new ArrayList<>();
    }

    public void addSong(UISong song){
        songs.add(song);
    }

    public long getID(){return ID;}

    //TODO: See what we can get from MediaStore for this
    public String getSecondaryText(){return songs.size() + " songs";}

    public String getPrimaryText(){return title;}

    public String getImageURL(){
        for(MusicData data : songs){
            if(data.getImageURL() != null &&!data.getImageURL().equals("")) {
                return data.getImageURL();
            }
        }
        return "";
    }

    public int getType(){
        return GENRE;
    }

    public List<MusicData> getChildren(){
        return songs;
    }
}