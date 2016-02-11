package io.unisong.android.network.session;

import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.emitter.Emitter;
import io.unisong.android.activity.session.SessionSongsAdapter;
import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.MusicDataManager;
import io.unisong.android.audio.song.LocalSong;
import io.unisong.android.audio.song.Song;
import io.unisong.android.audio.song.UnisongSong;
import io.unisong.android.network.SocketIOClient;

/**
 * This class manages the queue of songs to be played. It supports both operations
 * from the UI and backend, and is capable of updating both.
 * Created by Ethan on 9/12/2015.
 */
public class SongQueue implements AudioObserver {

    private static final String LOG_TAG = SongQueue.class.getSimpleName();

    private List<Song> songQueue;
    private SessionSongsAdapter.IncomingHandler handler;
    private UnisongSession parentSession;
    private SocketIOClient socketIOClient;
    private AudioStatePublisher publisher;
    private Timer timer;

    private TimerTask checkManagerStatusTask = new TimerTask() {
        @Override
        public void run() {
            checkMusicLoadingStatus();
        }
    };

    public SongQueue(UnisongSession session){
        socketIOClient = SocketIOClient.getInstance();
        songQueue = new ArrayList<>();
        parentSession = session;
        publisher = AudioStatePublisher.getInstance();
        timer = new Timer();
    }

    /**
     * Adds a song to the current session queue
     * @param song - the song to be added
     */

    public void addSong(Song song){
        if(songQueue.indexOf(song) == -1)
            addSong(songQueue.size(), song);
    }

    // Adds a song and notifies the adapter
    public void addSong(int position, Song song){

        songQueue.add(position, song);

        sendAdd(position, song);

    }



    public List<Song> getQueue(){
        return songQueue;
    }

    public void deleteSong(int songID){
        Song songToRemove = null;
        for(Song song : songQueue){
            if(song.getID() == songID){
                songToRemove = song;
            }
        }

        if(songToRemove != null)
            remove(songQueue.indexOf(songToRemove), false);

    }

    /**
     * Removes the song at the given position and updates the dataset.
     * @param updateServer - whether this action if from the UI - this variable tells us whether to update
     *               the UI or the server
     * @param position - the position to remove from
     */
    public void remove(int position, boolean updateServer){
        if(position == -1)
            return;


        Song songToRemove = songQueue.get(position);

        if(songToRemove != null) {
            songQueue.remove(songToRemove);

            sendRemove(position);

            if(parentSession.isMaster() && updateServer)
                parentSession.deleteSong(songToRemove.getID());

            songToRemove.destroy();
        }

    }

    public Song getSong(int songID){
        for(Song song : songQueue){
            if(song.getID() == songID){
                return song;
            }
        }
        return null;
    }

    public int size(){
        return songQueue.size();
    }

    /**
     * Updates the song queue with data from server.
     * @param songArray
     */
    public void update(JSONArray songArray, JSONArray queue){

        Runnable parse = () -> {parseUpdate(songArray , queue);};
        // TODO : figure out a way to
        // First we will update the songs
        Log.d(LOG_TAG, songArray.toString());
        MusicDataManager manager = MusicDataManager.getInstance();
        // TODO : revert isLoaded to manager.isDoneLoading()
        if( manager.isDoneLoading()){
            parseUpdate(songArray, queue);
        } else {
            this.songArray = songArray;
            this.queue = queue;
            timer.scheduleAtFixedRate(checkManagerStatusTask, 100, 1000);
        }
    }


    // temp variables for waiting for MusicDataManager to load
    private JSONArray songArray;
    private JSONArray queue;
    private void checkMusicLoadingStatus(){
        if(MusicDataManager.getInstance().isDoneLoading()){
            parseUpdate(songArray, queue);
            timer.cancel();
        }
    }

    private void parseUpdate(JSONArray songArray , JSONArray queue){
        Log.d(LOG_TAG , "Parsing SongQueue data");
        try {
            for (int i = 0; i < songArray.length(); i++) {
                Log.d(LOG_TAG , "Creating song with index i : " + i);
                JSONObject songJSON = songArray.getJSONObject(i);
                int ID = songArray.getJSONObject(i).getInt("songID");

                Song song = getSong(ID);
                if(song == null){
                    if(parentSession.isMaster()) {
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
            // same way?
            List<Song> orderedList = new ArrayList<>();
            for(int i = queue.length() - 1; i >= 0; i--){
                Song song = getSong(queue.getInt(i));
                if (song != null)
                    orderedList.add(0 , song);
            }

            songQueue = orderedList;

            sendChanged();

        } catch (JSONException e){
            e.printStackTrace();
        }

        Log.d(LOG_TAG , "Song updating finished, queue size is : " + songQueue.size());

        this.songArray = null;
        this.queue = null;
    }

    public void registerHandler(SessionSongsAdapter.IncomingHandler handler){
        this.handler = handler;
    }

    public Song getCurrentSong(){
        if(songQueue.size() == 0)
            return null;

        return songQueue.get(0);
    }

    /**
     * Moves a song from fromPosition to toPosition, unless we are currently playing
     * @param fromPosition
     * @param toPosition
     */
    public void move(int fromPosition, int toPosition){
        if(AudioStatePublisher.getInstance().getState() == AudioStatePublisher.PLAYING) {
            if (toPosition == 0)
                toPosition = 1;

            if(fromPosition == 0)
                return;
        }

        Song song = songQueue.get(fromPosition);
        songQueue.remove(fromPosition);
        songQueue.add(toPosition, song);

        parentSession.sendUpdate();
    }

    public JSONArray getJSONQueue(){
        JSONArray array = new JSONArray();

        for(int i = 0; i < songQueue.size(); i++){
            array.put(songQueue.get(i).getID());
        }

        return array;
    }

    public JSONArray getSongsJSON(){
        JSONArray array = new JSONArray();

        for(int i = 0; i < songQueue.size(); i++){
            array.put(songQueue.get(i).toJSON());
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
        if(handler == null)
            return;

        Message message = new Message();

        message.what = SessionSongsAdapter.ADD;
        message.arg1 = position;
        message.obj = song;

        handler.sendMessage(message);
    }

    private void sendRemove(int position){
        if(handler == null)
            return;

        Message message = new Message();

        message.what = SessionSongsAdapter.REMOVE;
        message.arg1 = position;

        handler.sendMessage(message);
    }

    private void sendChanged(){
        if(handler == null)
            return;

        Message message = new Message();
        message.what = SessionSongsAdapter.CHANGED;

        handler.sendMessage(message);
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
                parentSession.getUpdate();
            }
        } catch (JSONException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "Getting fields from JSONObject or updating the song failed!");
        }
    }

    @Override
    public void update(int state) {
        switch (state){
            case AudioStatePublisher.END_SONG:
                if(getSong(publisher.getSongToEnd()) != null)
                    remove(0 , true);
                break;
            case AudioStatePublisher.START_SONG:
                getCurrentSong().start();
                break;
            case AudioStatePublisher.SEEK:
                getCurrentSong().seek(publisher.getSeekTime());
        }
    }

}
