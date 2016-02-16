package io.unisong.android.activity.session.music_select;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by Ethan on 2/26/2015.
 */
public class UIPlaylist implements MusicData{

    private final static String LOG_TAG = UIPlaylist.class.getSimpleName();
    private long ID;
    private String title;
    private int count;
    private String data;
    private List<MusicData> songs;

    //The class for storing playlist information
    public UIPlaylist(long playlistID, String playlistName, String data) {
        ID = playlistID;
        title = playlistName;
        this.data = data;
        songs = new ArrayList<>();
        Log.d(LOG_TAG, "data : " + this.data);
    }

    public void addSong(UISong song){
        songs.add(song);
    }

    public long getID(){return ID;}

    //TODO: See what we can get from MediaStore for this
    public String getSecondaryText(){return songs.size() + " songs";}

    public String getPrimaryText(){return title;}

    public int getType(){
        return PLAYLIST;
    }

    // TODO : see about some sort of playlist imagery?
    public String getImageURL(){
        for(MusicData data : songs){
            if(data.getImageURL() != null && !data.getImageURL().equals("")) {
                return data.getImageURL();
            }
        }
        return "";
    }

    public List<MusicData> getChildren(){
        return songs;
    }
}
