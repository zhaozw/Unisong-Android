package io.unisong.android.network.session;

import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.song.Song;

/**
 * Created by Ethan on 9/12/2015.
 */
public class SongQueue {

    private HttpClient mClient;
    private List<Song> mSongQueue;

    public SongQueue(){
        mClient = HttpClient.getInstance();
        mSongQueue = new ArrayList<>();
    }

    /**
     * Adds a song to the current session queue
     * @param song - the song to be added
     */

    public void addSong(Song song){
        // TODO : add server code.
        mSongQueue.add(song);
    }

    /**
     * This method will create a song
     * @param song - the song to be created on the server
     */
    public void createSong(Song song){

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

    public int size(){
        return mSongQueue.size();
    }
}
