package io.unisong.android.network.session;

import java.util.LinkedList;
import java.util.List;

import io.unisong.android.network.Song;

/**
 * Created by Ethan on 9/12/2015.
 */
public class SongQueue {

    private List<Song> mSongQueue;

    public SongQueue(){
        mSongQueue = new LinkedList<>();
    }

    /**
     * Adds a song to the current session queue
     * @param song - the song to be added
     */

    public void addSong(Song song){
        mSongQueue.add(song);
    }

    public void removeSong(int songID){
        Song songToRemove = null;
        for(Song song : mSongQueue){
            if(song.getID() == songID){
                songToRemove = song;
            }
        }

        if(songToRemove == null)
            mSongQueue.remove(songToRemove);
    }

    public void removeSong(Song song){
        removeSong(song.getID());
    }

    public List<Song> getQueue(){
        return mSongQueue;
    }

    public Song getSong(int songID){
        for(Song song : mSongQueue){
            if(song.getID() == songID){
                return song;
            }
        }
        return null;
    }
}