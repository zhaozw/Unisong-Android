package io.unisong.android.network.session;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.unisong.android.activity.session.SessionSongsAdapter;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.song.LocalSong;
import io.unisong.android.network.song.Song;
import io.unisong.android.network.song.UnisongSong;

/**
 * Created by Ethan on 9/12/2015.
 */
public class SongQueue {

    private static final String LOG_TAG = SongQueue.class.getSimpleName();

    private HttpClient mClient;
    private List<Song> mSongQueue;
    private SessionSongsAdapter mAdapter;
    private UnisongSession mParentSession;


    public SongQueue(UnisongSession session){
        mClient = HttpClient.getInstance();
        mSongQueue = new ArrayList<>();
        mParentSession = session;
    }

    /**
     * Adds a song to the current session queue
     * @param song - the song to be added
     */

    public void addSong(Song song){
        // TODO : add server code.
        // TODO : find a way to differentiate between new songs and songs from server
        mSongQueue.add(song);
        if(mAdapter != null)
            mAdapter.add(song);
    }

    public void addSong(int position, Song song){
        mSongQueue.add(position , song);
        if(mAdapter != null)
            mAdapter.add(position , song);
    }

    public void removeSong(int songID){
        Song songToRemove = null;
        for(Song song : mSongQueue){
            if(song.getID() == songID){
                songToRemove = song;
            }
        }

        if(songToRemove != null) {
            mSongQueue.remove(songToRemove);
            if(mAdapter != null)
                mAdapter.remove(songToRemove);
        }
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

    /**
     * Updates the song queue with data from server.
     * @param songArray
     */
    public void update(JSONArray songArray, JSONArray queue){

        // TODO : figure out a way to
        // First we will update the songs
        Log.d(LOG_TAG, songArray.toString());
        try {
            for (int i = 0; i < songArray.length(); i++) {
                JSONObject songJSON = songArray.getJSONObject(i);
                int ID = songArray.getJSONObject(i).getInt("songID");

                Song song = getSong(ID);
                if(song == null){
                    if(mParentSession.isMaster()) {
                        LocalSong newSong = new LocalSong(songJSON);
                        addSong(newSong);
                    } else {
                        UnisongSong newSong = new UnisongSong(songJSON);
                        addSong(newSong);
                    }
                } else {
                    song.update(songJSON);
                }
            }


            // then we will update the order
            // TODO : figure out how to reorder adapter.
            // same way?

            for(int i = queue.length() - 1; i > 0; i++){
                Song song = getSong(queue.getInt(i));
                removeSong(song);
                addSong(0 , song);
            }

        } catch (JSONException e){
            e.printStackTrace();
        }

    }

    public void setAdapter(SessionSongsAdapter adapter){
        mAdapter = adapter;
    }
}
