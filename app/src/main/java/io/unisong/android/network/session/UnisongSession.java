package io.unisong.android.network.session;

import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import io.socket.emitter.Emitter;
import io.unisong.android.activity.UnisongActivity;
import io.unisong.android.activity.session.MainSessionActivity;
import io.unisong.android.activity.session.SessionSongsAdapter;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.network.Client;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.client.Listener;
import io.unisong.android.network.client.receiver.ServerReceiver;
import io.unisong.android.network.host.Broadcaster;
import io.unisong.android.network.host.transmitter.ServerTransmitter;
import io.unisong.android.network.host.transmitter.Transmitter;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.song.Song;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;
import io.unisong.android.network.user.UserUtils;

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

    private static UnisongActivity sActivityToNotify;
    private static UnisongSession sCurrentSession;

    public static UnisongSession getCurrentSession(){
        return sCurrentSession;
    }

    public static void setCurrentSession(UnisongSession session) {
        sCurrentSession = session;
        if(sActivityToNotify != null && session != null){
            sActivityToNotify.sessionLoaded();
            sActivityToNotify = null;
        }
        if(session != null){
            session.configureSocketIO();
        }
    }

    public static void notifyWhenLoaded(UnisongActivity activty){
        sActivityToNotify = activty;
    }

    private int mNewSongID, mSessionID;
    private Song mCurrentSong;

    private boolean mIsMaster, mSocketIOConfigured;

    private SongQueue mSongQueue;
    private String mMaster , mSessionState;

    private MainSessionActivity.SessionMessageHandler mSessionHandler;
    private SocketIOClient mSocketIOClient;
    private SessionMembers mMembers;
    private List<Client> mClients;
    private HttpClient mClient;
    private SessionSongsAdapter mAdapter;

    /**
     * This constructor creates a UnisongSession where the current user is the
     * session host.
     */
    public UnisongSession(){

        mClient = HttpClient.getInstance();
        mSocketIOClient = SocketIOClient.getInstance();

        mSongQueue = new SongQueue(this);
        mMembers = new SessionMembers();
        mIsMaster = true;

        mMaster = CurrentUser.getInstance().getUUID().toString();

        create();
        mNewSongID = 0;

        mSocketIOConfigured = false;
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
        mMembers = new SessionMembers();

        mSocketIOConfigured = false;
        getInfoFromServer();

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
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void parseJSONObject(JSONObject object) throws JSONException{

        try {
            if (object.has("master")) {
                mMaster = object.getString("master");
                User user = CurrentUser.getInstance();
                // If the user is null, then let's just wait for it
                // TODO : ensure that this doesn't break any vital components

                while (user == null) {
                    try {
                        synchronized (this) {
                            this.wait(1);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    user = CurrentUser.getInstance();
                    // TODO : moderate this logging
                    Log.d(LOG_TAG, "waiting for CurrentUser");
                }
                Log.d(LOG_TAG, "CurrentUser retrieved");
                if (user.getUUID() == null)
                    return;

                if (user.getUUID().compareTo(UUID.fromString(mMaster)) == 0) {
                    mIsMaster = true;

                    // If we are the master and are getting updates, start u
                    if (Broadcaster.getInstance() == null) {
                        Broadcaster broadcaster = new Broadcaster(this);
                        broadcaster.addTransmitter(new ServerTransmitter());
                    } else {
                        Broadcaster broadcaster = Broadcaster.getInstance();
                        List<Transmitter> transmitters = broadcaster.getTransmitters();
                        boolean hasServer = false;
                        for (Transmitter obj : transmitters) {
                            hasServer = obj.getClass().equals(ServerTransmitter.class);

                            if (hasServer)
                                break;
                        }

                        if (!hasServer)
                            broadcaster.addTransmitter(new ServerTransmitter());
                    }

                } else {
                    if (Listener.getInstance() == null) {
                        Listener listener = new Listener(this);
                        listener.addReceiver(new ServerReceiver(listener));
                    }
                }
            }

            if (object.has("users")) {
                JSONArray array = object.getJSONArray("users");
                mMembers.update(array);
            }

            if (object.has("songs") && object.has("queue")) {
                JSONArray songArray = object.getJSONArray("songs");
                JSONArray queueArray = object.getJSONArray("queue");
                mSongQueue.update(songArray, queueArray);
            }

            // TODO : ensure that this works in sync with Listener.
            // TODO : figure out how to know when to update the Master's audiosessionstate, and when to
            // update the server instead
            if (object.has("sessionState") && !isMaster()) {
                mSessionState = object.getString("sessionState");
                AudioStatePublisher publisher = AudioStatePublisher.getInstance();

                if (mSessionState.equals("idle")) {
                    if (publisher.getState() != AudioStatePublisher.IDLE) {
                        publisher.update(AudioStatePublisher.IDLE);
                    }
                } else if (mSessionState.equals("paused")) {
                    if (publisher.getState() != AudioStatePublisher.PAUSED) {
                        publisher.update(AudioStatePublisher.PAUSED);
                    }
                } else if (mSessionState.equals("playing")) {
                    if (publisher.getState() != AudioStatePublisher.PLAYING) {
                        publisher.update(AudioStatePublisher.PLAYING);
                    }
                }
            }

            if (object.has("songID")) {
                mNewSongID = object.getInt("songID");
            }

            Log.d(LOG_TAG, "Parsing complete");
        } catch (Exception e){
            e.printStackTrace();
            Log.d(LOG_TAG , "Unknown Exception in UnisongSession.parseJSON!");
        }
    }

    /**
     * Creates the session on the server side.
     */
    private void create(){
        getCreateThread().start();
    }

    public void configureSocketIO(){
        if(mSocketIOConfigured)
            return;

        mSocketIOConfigured = true;
        mSocketIOClient.on("user joined" , mUserJoined);
        mSocketIOClient.on("update session", mUpdateSessionListener);
        mSocketIOClient.on("user left" , mUserLeft);
        mSocketIOClient.on("end session" , mEndSession);
        mSocketIOClient.on("kick" , mKickListener);
        mSocketIOClient.on("kick result" , mKickResultListener);
        Log.d(LOG_TAG, "Configured Socket.IO");
    }

    public int incrementNewSongID(){
        int oldSongID = mNewSongID;
        mNewSongID++;
        return oldSongID;
    }


    private Thread getCreateThread(){
        return new Thread(() ->{
            Response response;
            Log.d(LOG_TAG , "Creating Unisong session.");
            try {
                response = mClient.syncPost(NetworkUtilities.HTTP_URL + "/session/", new JSONObject());

                if(response.code() == 200){
                    String body = response.body().string();
                    JSONObject object = new JSONObject(body);
                    Log.d(LOG_TAG , object.toString());
                    mSessionID = object.getInt("sessionID");
                    Log.d(LOG_TAG , "Session ID : " + mSessionID);

                    mSocketIOClient.joinSession(mSessionID);
                    mMembers.add(CurrentUser.getInstance());
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
        if(mSongQueue != null)
            return mSongQueue.getCurrentSong();

        return null;
    }

    public void startSong(int songID){
        mCurrentSong = mSongQueue.getSong(songID);
        if(mIsMaster){
            Log.d(LOG_TAG , "Sending start song.");
            Broadcaster.getInstance().startSong(mCurrentSong);
        }
    }

    public void endSession(){

    }

    public void disconnect(){
        mSocketIOClient.emit("leave" , getSessionID());
    }

    public void destroy(){
        if(mIsMaster)
            mSocketIOClient.emit("end session" , getSessionID());

        disconnect();

        mSongQueue = null;
        mClients = null;
    }

    public void addFrame(AudioFrame frame){
        Song song = mSongQueue.getSong(frame.getSongID());
        // TODO : store frames without a song in case we miss a create song
        if(song != null)
            song.addFrame(frame);

    }

    public SessionMembers getMembers(){
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

        Log.d(LOG_TAG, "Creating song on server");

        // If we are the master, then notify the server. If not, then we are simply responding.
        if(isMaster())
            mSocketIOClient.emit("add song" , song.toJSON());
    }

    /**
     * Remove a song from the SongSession, with an ID. calls "delete song" in the socket.io client
     * if we are the session master
     * @param ID - the ID of the  given song
     */
    public void deleteSong(int ID){
        mSongQueue.deleteSong(ID);

        Object[] args = new Object[2];
        args[0] = ID;
        args[1] = mSessionID;

        if(isMaster())
            mSocketIOClient.emit("delete song" , args);
    }

    private Emitter.Listener mUpdateSessionListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                JSONObject object = (JSONObject) args[0];
                parseJSONObject(object);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };


    /**
     * Updates the session with information from the server.
     */
    public void getUpdate(){
        mSocketIOClient.emit("get session" , getSessionID());
    }

    public void sendUpdate(){
        Object[] array = new Object[2];
        array[0] = mSessionID;
        array[1] = this.toJSON();
        mSocketIOClient.emit("update session" , array);

        mSocketIOClient.emit("update songs" , mSongQueue.getSongsJSON());
    }

    public boolean isMaster(){
        return mIsMaster;
    }

    public void leave(){
        if(isMaster()){

            Broadcaster broadcaster = Broadcaster.getInstance();

            if(broadcaster != null){
                broadcaster.destroy();
            }
            destroy();

        } else {
            Listener listener = Listener.getInstance();
            if(listener != null)
                listener.destroy();

        }

        setCurrentSession(null);
    }

    /**
     * Broadcasts a 'kick' event to the Socket.IO server to kick the
     * selected user. Will only work if the current user is the session master
     * and authenticated
     * @param user
     */
    public void kick(User user){
        mSocketIOClient.emit("kick", user.getUUID().toString());
    }

    /**
     * Listens for the result of attempting to kick a user
     * args ->
     * args[0] - JSONObject:
     * {
     *     code : HTTP status code,
     *     response : descriptive but probably unhelpful string
     * }
     */
    private Emitter.Listener mKickResultListener = (Object[] args) -> {
        try{
            JSONObject response = (JSONObject) args[0];

            if(response.getInt("code") == 200){
                Log.d(LOG_TAG, "Kick successful! Response received");

                String uuid = response.getString("response");

                User user = UserUtils.getUser(uuid);

                Log.d(LOG_TAG , user.toString() + " to be kicked");
                mMembers.remove(user);

            } else if(response.getInt("code") == 400){
                Log.d(LOG_TAG , "Bad request on kick! Response : " + response.getString("response"));
            } else if(response.getInt("code") == 403) {
                Log.d(LOG_TAG , "Unauthorized! Are we not the session master?");
            }

        } catch (NullPointerException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "NullPointerException in KickResultListener! Bad Parameters?");
        } catch (ClassCastException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "ClassCastException in KickResultListener! Bad documentation or server bug?");
        } catch (JSONException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "JSON parsing failed in KickResultListener!");
        }
    };

    /**
     * Listens for a user being kicked. If that user happens to be the current one, then remove
     * us from the session.
     *
     * args ->
     * args[0] - String uuid - the UUID of the user to be kicked
     */
    private Emitter.Listener mKickListener = (Object[] args) -> {
        try{
            String uuid = (String) args[0];

            User user = UserUtils.getUser(uuid);

            Log.d(LOG_TAG , "Kick received for user : " + user);

            if(mMembers.contains(user)){
                mMembers.remove(user);
            }

            if(user.equals(CurrentUser.getInstance())){
                leave();
                // TODO : remove us from the session and move us back to the main screen
                // to do this we'll need a broadcast receiver in the invite friends activity and MainSessionActivty
                if(mSessionHandler != null) {
                    Message message = new Message();
                    message.what = MainSessionActivity.KICKED;
                    mSessionHandler.sendMessage(message);
                }
                Log.d(LOG_TAG, "We are being kicked!");
            }

        } catch (ClassCastException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "Casting failed in KickListener!");
        } catch (NullPointerException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "NullPointerException in KickListener! Is CurrentUser.sInstance null?");
        }
    };

    public JSONObject toJSON(){
        JSONObject object = new JSONObject();
        try {
            JSONArray users = new JSONArray();

            for (User user : mMembers.getList()) {
                users.put(user.getUUID().toString());
            }


            object.put("users", users);

            object.put("queue" , mSongQueue.getJSONQueue());
            object.put("songs" , mSongQueue.getSongsJSON());

            object.put("master" , mMaster);

            if(sCurrentSession == this){

                AudioStatePublisher publisher = AudioStatePublisher.getInstance();

                if(publisher.getState() == AudioStatePublisher.IDLE){
                    object.put("sessionState" , "idle");
                } else if(publisher.getState()  == AudioStatePublisher.PAUSED){
                    object.put("sessionState" , "idle");
                } else if(publisher.getState() == AudioStatePublisher.PLAYING){
                    object.put("sessionState" , "playing");
                }
            } else {
                object.put("sessionState" , mSessionState);
            }

        } catch (JSONException e){
            e.printStackTrace();
        }

        return object;
    }


    public void updateCurrentSong(){
        mCurrentSong = mSongQueue.getCurrentSong();
    }


    // The listener for when a user joins a session
    private Emitter.Listener mUserJoined = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "User joined received");
            try {
                String UUID = (String) args[0];

                User user = UserUtils.getUser(java.util.UUID.fromString(UUID));

                mMembers.add(user);
                Log.d(LOG_TAG, "User " + user.toString() + "  joined session!");
            } catch (ClassCastException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "Format error in 'user joined'");
            }
        }
    };

    // The listener for when a user leaves a session
    private Emitter.Listener mUserLeft = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "user left received");
            try {
                String UUID = (String) args[0];

                User user = UserUtils.getUser(java.util.UUID.fromString(UUID));

                mMembers.remove(user);
            } catch (ClassCastException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "Format error in 'user joined'");
            }
        }
    };


    // The listener for when a user leaves a session
    private Emitter.Listener mEndSession = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                int sessionID = (Integer) args[0];

                if(mSessionID == sessionID && !isMaster())
                    endSession();

            } catch (ClassCastException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "Format error in 'end session'");
            }
        }
    };


    public void updateSong(JSONObject object){
        mSongQueue.updateSong(object);
    }

    public void setSessionActivityHandler(MainSessionActivity.SessionMessageHandler handler){
        mSessionHandler = handler;
    }

}