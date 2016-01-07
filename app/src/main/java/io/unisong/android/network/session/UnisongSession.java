package io.unisong.android.network.session;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.socket.emitter.Emitter;
import io.unisong.android.activity.session.SessionSongsAdapter;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.AudioTrackManager;
import io.unisong.android.network.Client;
import io.unisong.android.network.Host;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.host.Broadcaster;
import io.unisong.android.network.song.Song;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.song.UnisongSong;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;

/**
 * A network session between a variety of devices.
 * This class will contain all the information about the session and those
 * within it, along with the information on current and future songs.
 *
 * Utilizes the Singleton design pattern.
 * Created by Ethan on 8/1/2015.
 */
public class UnisongSession {

    private final static String LOG_TAG = UnisongSession.class.getSimpleName();

    private static UnisongSession sCurrentSession;

    public static UnisongSession getCurrentSession(){
        return sCurrentSession;
    }

    public static void setCurrentSession(UnisongSession session) {
        sCurrentSession = session;
    }

    private int mNewSongID;
    private Song mCurrentSong;
    private int mSessionID;
    private boolean mIsLocalSession;
    private SongQueue mSongQueue;
    private boolean mIsMaster;
    private boolean mIsDisconnected;

    private SocketIOClient mSocketIOClient;
    private List<User> mMembers;
    private List<Client> mClients;
    private HttpClient mClient;
    private Host host;
    private SessionSongsAdapter mAdapter;

    /**
     * This constructor creates a UnisongSession where the current user is the
     * session host.
     */
    public UnisongSession(){

        mClient = HttpClient.getInstance();
        mSocketIOClient = SocketIOClient.getInstance();
        if(mSocketIOClient != null){
            mSocketIOClient.on("add song" , mAddSongListener);
        }

        mSongQueue = new SongQueue(this);
        mMembers = new ArrayList<>();
        mIsMaster = true;

        create();
        configureSocketIO();
        mIsDisconnected = false;
        mNewSongID = 0;

        Broadcaster broadcaster = new Broadcaster(this);
    }

    /**
     * Creates a UnisongSession with only an ID and then populates all of the fields from the server.
     */
    public UnisongSession(int ID){
        mSessionID = ID;

        mNewSongID = 0;
        /*
        try{
            throw new Exception();
        } catch (Exception e){
            e.printStackTrace();
        }*/
        Log.d(LOG_TAG , "Creating session based on ID : " + ID);
        mClient = HttpClient.getInstance();
        mSocketIOClient = SocketIOClient.getInstance();

        mSongQueue = new SongQueue(this);
        mMembers = new ArrayList<>();

        configureSocketIO();
        getInfoFromServer();
        mIsDisconnected = false;

    }

    public UnisongSession(User userToJoin){

    }

    /**
     * Starts a thread to download the relevant issues from the server
     */
    private void getInfoFromServer(){
        Log.d(LOG_TAG , "Posting");
        // TODO : don't use thread here.
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG , "calling download");
                Looper.prepare();
                downloadInfo();
            }
        }).start();
    }

    /**
     * Downloads the info about the current info from the server
     */
    private void downloadInfo(){
        try {
            Log.d(LOG_TAG , "Sending GET about session.");
            Response response = mClient.syncGet(NetworkUtilities.HTTP_URL + "/session/" + mSessionID);

            Log.d(LOG_TAG , "Code : " + response.code() );
            if(response.code() == 200){
                String body = response.body().string();
                Log.d(LOG_TAG , body);

                JSONObject object = new JSONObject(body);

                parseJSONObject(object);
            } else if(response.code() == 404){
                Log.d(LOG_TAG , "Session not found!");
            }


            Log.d(LOG_TAG , "Sending GET about session songID.");
            response = mClient.syncGet(NetworkUtilities.HTTP_URL + "/session/" + mSessionID + "/songID");

            String body = response.body().string();

            Log.d(LOG_TAG , "SongID Response Body: " + body);
            mNewSongID = Integer.parseInt(body) + 1;
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void parseJSONObject(JSONObject object) throws JSONException{

        if(object.has("master")){
            String masterID = object.getString("master");
            User user = CurrentUser.getInstance();
            if(user.getUUID().compareTo(UUID.fromString(masterID)) == 0){
                mIsMaster = true;
                if(Broadcaster.getInstance() == null) {
                    Broadcaster broadcaster = new Broadcaster(this);
                }
            }
        }

        if(object.has("users")){

        }

        if(object.has("songs") && object.has("queue")){
            JSONArray songArray = object.getJSONArray("songs");
            JSONArray queueArray = object.getJSONArray("queue");
            mSongQueue.update(songArray, queueArray);
        }

        if(object.has("sessionState")){

        }

        if(object.has("queue")){

        }
    }

    /**
     * Creates the session on the server side.
     */
    private void create(){
        getCreateThread().start();
    }

    private void configureSocketIO(){
        if(this == sCurrentSession) {
            mSocketIOClient.on("add song", mAddSongListener);
            mSocketIOClient.on("get session" , mGetSessionListener);
        }
    }

    public int incrementNewSongID(){
        int oldSongID = mNewSongID;
        mNewSongID++;
        return oldSongID;
    }

    public void setSongAdapter(SessionSongsAdapter adapter){
        mAdapter = adapter;
        mSongQueue.setAdapter(adapter);
    }


    private Thread getCreateThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                Response response;
                Log.d(LOG_TAG , "Creating Unisong session.");
                try {
                    response = mClient.syncPost(NetworkUtilities.HTTP_URL + "/session/", new JSONObject());

                    if(response.code() == 200){
                        String body = response.body().string();
                        JSONObject object = new JSONObject(body);
                        mSessionID = object.getInt("sessionID");
                        Log.d(LOG_TAG , "Session ID : " + mSessionID);
                    }
                } catch (IOException e){
                    e.printStackTrace();
                    return;
                } catch (JSONException e){
                    Log.d(LOG_TAG, "JSON Parsing failed.");
                }

                try {
                    JSONObject credentials = new JSONObject();
                    User user = CurrentUser.getInstance();
                    credentials.put("username" , user.getUsername());
                    credentials.put("password" , user.getPassword());
                    mSocketIOClient.emit("authenticate" , credentials);
                } catch (JSONException e){
                    e.printStackTrace();
                }

            }
        });
    }

    public void addClient(Client client){
        for (Client comp : mClients){
            if(comp.equals(client)) return;
        }

        mClients.add(client);
    }

    public int getCurrentSongID(){
        return mCurrentSong.getID();
    }

    public Song getCurrentSong(){
        return mCurrentSong;
    }

    public void startSong(int songID){
        mCurrentSong = mSongQueue.getSong(songID);
        if(mIsMaster){
            Log.d(LOG_TAG , "Sending start song.");
            Broadcaster.getInstance().startSong(mCurrentSong);
        }
    }

    public void endSession(){
        // TODO : end session and disconnect hosts
        // can only do if host
        mSocketIOClient.emit("end session", new JSONObject());
    }

    public void disconnect(){
        // TODO : disconnects user from session

    }

    public void destroy(){
        if(mIsMaster){
            endSession();
        }
        disconnect();

        mSongQueue = null;
        mClients = null;
        mIsDisconnected = true;
    }

    public void addFrame(AudioFrame frame){
        Song song = mSongQueue.getSong(frame.getSongID());
        song.addFrame(frame);
    }

    public List<User> getMembers(){
        return mMembers;
    }

    public SongQueue getSongQueue(){return mSongQueue;}

    public int getSessionID(){
        return mSessionID;
    }

    public void addSong(Song song){
        mSongQueue.addSong(song);


        if(mSongQueue.size() == 1 && mIsMaster){
            Log.d(LOG_TAG, "The first song has been added, automatically playing");
            mCurrentSong = song;

            // TODO : investigate if the first song should auto-play
            //startSong(song.getID());
        }

        Log.d(LOG_TAG , "Creating song on server");
        mSocketIOClient.emit("add song" , song.getJSON());
    }

    private Emitter.Listener mGetSessionListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                JSONObject object = (JSONObject) args[0];
                parseJSONObject(object);
            } catch (Exception e){
                e.printStackTrace();
                // TODO : handle individual exceptions and report them to crashalytics/google analytics
            }
        }
    };

    private Emitter.Listener mAddSongListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if(isMaster())
                    return;

                JSONObject object = (JSONObject) args[0];
                String type = object.getString("type");
                if(type.equals(UnisongSong.TYPE_STRING)){
                    UnisongSong song = new UnisongSong(object);
                    mSongQueue.addSong(song);
                }
                // TODO : when added add SoundcloudSong and Spotify/Google play songs.
            } catch (Exception e){
                e.printStackTrace();
                // TODO : handle individual exceptions and report them to crashalytics/google analytics
            }
        }
    };

    /**
     * Updates the session with information from the server.
     */
    public void getUpdate(){
        mSocketIOClient.emit("get session" , getSessionID());
    }

    public boolean isMaster(){
        return mIsMaster;
    }

}