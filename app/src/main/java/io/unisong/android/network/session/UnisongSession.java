package io.unisong.android.network.session;

import android.util.Log;

import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.socket.emitter.Emitter;
import io.unisong.android.PrefUtils;
import io.unisong.android.activity.session.SessionSongsAdapter;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.network.Client;
import io.unisong.android.network.Host;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.song.LocalSong;
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
    private static UnisongSession sInstance;

    public static UnisongSession getInstance(){
        return sInstance;
    }

    private Song mCurrentSong;
    private String mSessionID;
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

        mSongQueue = new SongQueue();
        mMembers = new ArrayList<>();
        mIsMaster = true;

        create();
        configureSocketIO();
        mIsDisconnected = false;
        sInstance = this;


    }

    /**
     * Creates a UnisongSession with only an ID and then populates all of the fields from the server.
     */
    public UnisongSession(int ID){
        mSessionID = ID + "";

        Log.d(LOG_TAG , "Creating session based on ID : " + ID);
        mClient = HttpClient.getInstance();
        mSocketIOClient = SocketIOClient.getInstance();

        mSongQueue = new SongQueue();
        mMembers = new ArrayList<>();

        create();
        configureSocketIO();
        mIsDisconnected = false;
        sInstance = this;
    }

    public UnisongSession(User userToJoin){

    }

    /**
     * Creates the session on the server side.
     */
    private void create(){
        getCreateThread().start();
    }

    private void configureSocketIO(){

    }

    public void setSongAdapter(SessionSongsAdapter adapter){
        mAdapter = adapter;
    }


    private Thread getCreateThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                Response response;
                Log.d(LOG_TAG , "Creating Unisong session.");
                try {
                    response = mClient.post(NetworkUtilities.HTTP_URL + "/session/", new JSONObject());

                    if(response.code() == 200){
                        String body = response.body().string();
                        JSONObject object = new JSONObject(body);
                        mSessionID = object.getInt("sessionID") + "";
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
    }

    public void endSession(){
        // TODO : end session and disconnect hosts
        // can only do if host
    }

    public void disconnect(){
        // TODO : disconnects user from session

    }

    public void destroy(){
        if(mIsMaster){
            endSession();
        }
        disconnect();

        sInstance = null;

        mSongQueue = null;
        mClients = null;
        mIsDisconnected = true;
    }

    public void addFrame(AudioFrame frame){
        mSongQueue.getSong(frame.getSongID());
    }

    public List<User> getMembers(){
        return mMembers;
    }

    public SongQueue getSongQueue(){return mSongQueue;}

    public String getSessionID(){
        return mSessionID;
    }

    public void addSong(Song song){
        mSongQueue.addSong(song);
        if(mSongQueue.size() == 1 && mIsMaster){
            // TODO : start song
        }
        if(mAdapter != null){
            mAdapter.add(song);
        }
    }

    private Emitter.Listener mAddSongListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                JSONObject object = (JSONObject) args[0];
                String type = object.getString("type");
                if(type.equals(UnisongSong.TYPE_STRING)){

                } else if(type.equals(LocalSong.TYPE_STRING)){

                }// TODO : when added add SoundcloudSong and Spotify/Google play songs.
            } catch (Exception e){
                e.printStackTrace();
                // TODO : handle individual exceptions and report them to crashalytics/google analytics
            }
        }
    };

}
