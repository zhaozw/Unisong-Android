package io.unisong.android.activity.session.music_select;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 2/26/2015.
 */
public class UIArtist implements MusicData{

    private long ID;
    private String name;
    private List<MusicData> albums;

    //The class for storing artist data
    public UIArtist(long artistID, String artistName) {
        ID = artistID;
        name = artistName;
        albums = new ArrayList<>();
    }

    public void addAlbum(UIAlbum album){
        albums.add(album);
    }

    public long getID(){return ID;}

    public String getPrimaryText(){return name;}

    //The string that will be below the name
    public String getSecondaryText(){
        return albums.size() + " albums, " + getNumSongs() + " songs";
    }

    public int getNumSongs(){
        int songs = 0;
        for(MusicData album : albums){
            songs += ((UIAlbum)album).getSongs().size();
        }
        return songs;
    }

    public String getImageURL(){
        for(MusicData data : albums){
            if(data.getImageURL() != null && !data.getImageURL().equals("")) {
                return data.getImageURL();
            }
        }
        return "";
    }

    public int getType(){
        return ARTIST;
    }

    @Override
    public List<MusicData> getChildren() {
        return albums;
    }

}
