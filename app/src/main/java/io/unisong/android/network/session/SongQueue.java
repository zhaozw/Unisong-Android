package io.unisong.android.network.session;

import android.os.Message;
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
    private SessionSongsAdapter.IncomingHandler mHandler;
    private UnisongSession mParentSession;
    private SocketIOClient mSocketIOClient;


    public SongQueue(UnisongSession session){
        mClient = HttpClient.getInstance();
        mSocketIOClient = SocketIOClient.getInstance();
        mSongQueue = new ArrayList<>();
        mParentSession = session;
    }

    /**
     * Adds a song to the current session queue
     * @param song - the song to be added
     */

    public void addSong(Song song){
        if(mSongQueue.indexOf(song) == -1)
            addSong(mSongQueue.size(), song);
    }

    // Adds a song and notifies
    public void addSong(int position, Song song){
        mSongQueue.add(position, song);

        sendAdd(position, song);
    }

    public void deleteSong(int songID){
        Song songToRemove = null;
        for(Song song : mSongQueue){
            if(song.getID() == songID){
                songToRemove = song;
            }
        }

        if(songToRemove != null) {
            remove(mSongQueue.indexOf(songToRemove), false);
        }
    }

    public void deleteSong(Song song){
        deleteSong(song.getID());
    }

    public List<Song> getQueue(){
        return mSongQueue;
    }

    /**
     * Removes the song at the given position and updates the dataset.
     * @param fromUI - whether this action if from the UI - this variable tells us whether to update
     *               the UI or the server
     * @param position - the position to remove from
     */
    public void remove(int position, boolean fromUI){
        if(position == -1)
            return;

        Song songToRemove = mSongQueue.get(position);

        if(songToRemove != null) {
            mSongQueue.remove(songToRemove);

            sendRemove(position);

            if(mParentSession.isMaster() && fromUI)
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
            List<Song> orderedList = new ArrayList<>();
            for(int i = queue.length() - 1; i >= 0; i--){
                Song song = getSong(queue.getInt(i));
                if (song != null)
                    orderedList.add(0 , song);
            }

            mSongQueue = orderedList;

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

    public void registerHandler(SessionSongsAdapter.IncomingHandler handler){
        mHandler = handler;
    }

    public Song getCurrentSong(){
        if(mSongQueue.size () == 0)
            return null;

        return mSongQueue.get(0);
    }

    public void move(int fromPosition, int toPosition){
        Song song = mSongQueue.get(fromPosition);
        mSongQueue.remove(fromPosition);
        mSongQueue.add(toPosition, song);

        if(fromPosition == 0 || toPosition == 0)
            mParentSession.updateCurrentSong();

        mParentSession.sendUpdate();
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

    private Emitter.Listener mUpdateSongListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                JSONObject object = (JSONObject) args[0];

                int songID = object.getInt("songID");

                Song song = getSong(songID);

                song.update(object);

            } catch (JSONException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "JSON parsed incorrectly!");
            } catch (ClassCastException e){
                Log.d(LOG_TAG , "Format was wrong for 'song update'");
            }
        }
    };
    private void sendAdd(int position, Song song){
        if(mHandler == null)
            return;

        Message message = new Message();

        message.what = SessionSongsAdapter.ADD;
        message.arg1 = position;
        message.obj = song;

        mHandler.sendMessage(message);
    }

    private void sendRemove(int position){
        if(mHandler == null)
            return;

        Message message = new Message();

        message.what = SessionSongsAdapter.REMOVE;
        message.arg1 = position;

        mHandler.sendMessage(message);
    }

    public void updateSong(JSONObject object){
        try{
            int songID = object.getInt("songID");


            Song song = getSong(songID);

            if(song != null){
                song.update(object);
            } else {
                song = new UnisongSong(object);
                addSong(song);
                mParentSession.getUpdate();
            }
        } catch (JSONException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "Getting fields from JSONObject or updating the song failed!");
        }
    }

}
