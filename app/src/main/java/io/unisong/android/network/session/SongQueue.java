package io.unisong.android.network.session;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.emitter.Emitter;
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
    private SocketIOClient mSocketIOClient;


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
        addSong(mSongQueue.size(), song);
    }

    public void addSong(int position, Song song){
        mSongQueue.add(position, song);
        if(mAdapter != null)
            mAdapter.add(position , song);
    }

    public void deleteSong(int songID){
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

    public void deleteSong(Song song){
        deleteSong(song.getID());
    }

    public List<Song> getQueue(){
        return mSongQueue;
    }

    /**
     * Removes the song at the given position and updates the dataset
     * @param position
     */
    public void remove(int position){
        Song songToRemove = mSongQueue.get(position);

        if(songToRemove != null) {
            mSongQueue.remove(songToRemove);
            if(mAdapter != null)
                mAdapter.remove(songToRemove);

            mParentSession.deleteSong(songToRemove.getID());
        }

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
                        getSongLoop(songJSON);
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

            for(int i = queue.length() - 1; i > 0; i--){
                Song song = getSong(queue.getInt(i));
                if(mSongQueue.indexOf(song) != i) {
                    if (song != null) {
                        deleteSong(song);
                        addSong(0, song);
                    }
                }
            }

        } catch (JSONException e){
            e.printStackTrace();
        }

    }

    /**
     * Waits for MusicDataManager to load.
     * @param songJSON
     */
    private void getSongLoop(JSONObject songJSON){
        try {
            LocalSong newSong = new LocalSong(songJSON);
            addSong(newSong);
        } catch (Exception e){
            synchronized (this){
                try{
                    this.wait(20);
                } catch (InterruptedException dc){

                }
            }
            getSongLoop(songJSON);
        }
    }

    public void setAdapter(SessionSongsAdapter adapter){
        mAdapter = adapter;
    }

    public Song getCurrentSong(){
        return mSongQueue.get(0);
    }

    public void move(int fromPosition, int toPosition){
        Song song = mSongQueue.get(fromPosition);
        mSongQueue.remove(fromPosition);
        mSongQueue.add(toPosition , song);
    }

    public JSONArray getJSONQueue(){
        JSONArray array = new JSONArray();

        for(int i = 0; i < mSongQueue.size(); i++){
            array.put(mSongQueue.get(i).getID());
        }

        return array;
    }

    public JSONArray getSongsJSON(){
        JSONArray array = new JSONArray();

        for(int i = 0; i < mSongQueue.size(); i++){
            array.put(mSongQueue.get(i).toJSON());
        }

        return array;
    }

}
