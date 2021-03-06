package io.unisong.android.network.session;

import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import io.socket.emitter.Emitter;
import io.unisong.android.activity.UnisongActivity;
import io.unisong.android.activity.session.MainSessionActivity;
import io.unisong.android.activity.session.SessionSongsAdapter;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.song.Song;
import io.unisong.android.network.Client;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.client.Listener;
import io.unisong.android.network.client.receiver.ServerReceiver;
import io.unisong.android.network.host.Broadcaster;
import io.unisong.android.network.host.transmitter.ServerTransmitter;
import io.unisong.android.network.host.transmitter.Transmitter;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.ntp.TimeManager;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;
import io.unisong.android.network.user.UserUtils;

/**
 * A network session between a variety of devices.
 * This class will contain all the information about the session and those
 * within it, along with the information on current and future songs.
 *
 * Utilizes the Singleton design pattern for the current session.
 * Created by Ethan on 8/1/2015.
 */
public class UnisongSession {

    private final static String LOG_TAG = UnisongSession.class.getSimpleName();

    private static UnisongActivity activityToNotify;
    private static UnisongSession currentSession;
    private FragmentActivity refreshActivity;

    public static UnisongSession getCurrentSession(){
        return currentSession;
    }

    public static void setCurrentSession(UnisongSession session) {
        currentSession = session;

        // Attach the song Queue and other components to the AudioStatePublisher


        if(activityToNotify != null && session != null){
            activityToNotify.sessionLoaded();
            activityToNotify = null;
        }
        if(session != null){
            AudioStatePublisher.getInstance().attach(session.getSongQueue());
            session.configureSocketIO();
            session.playIfPlaying();
        }
    }

    public static void notifyWhenLoaded(UnisongActivity activty){
        activityToNotify = activty;
    }

    private int newSongID, sessionID;

    private boolean isMaster, socketIOConfigured, isLoaded = false;

    private SongQueue songQueue;
    private String master, sessionState;

    private MainSessionActivity.SessionMessageHandler sessionHandler;
    private AudioStatePublisher publisher = AudioStatePublisher.getInstance();
    private SocketIOClient socketIOClient = SocketIOClient.getInstance();
    private SessionMembers members;
    private List<Client> clients;
    private HttpClient client = HttpClient.getInstance();
    private SessionSongsAdapter adapter;
    private long lastUpdate;
    private Timer timer;

    // This is the layout that gets notified when we have received an update.
    private SwipeRefreshLayout swipeRefresh;

    /**
     * This constructor creates a UnisongSession where the current user is the
     * session host.
     */
    public UnisongSession(){
        songQueue = new SongQueue(this);
        members = new SessionMembers(this);
        isMaster = true;

        master = CurrentUser.getInstance().getUUID().toString();

        create();
        newSongID = 0;

        socketIOConfigured = false;
        Broadcaster broadcaster = new Broadcaster(this);
        broadcaster.addTransmitter(new ServerTransmitter(this));
        timer = new Timer();

    }

    /**
     * Creates a UnisongSession with only an ID and then populates all of the fields from the server.
     */
    public UnisongSession(int ID){
        sessionID = ID;

        newSongID = 0;
        /*
        try{
            throw new Exception();
        } catch (Exception e){
            e.printStackTrace();
        }*/
        Log.d(LOG_TAG , "Creating session based on ID : " + ID);

        songQueue = new SongQueue(this);
        members = new SessionMembers(this);

        socketIOConfigured = false;
        getInfoFromServer();
        timer = new Timer();

    }

    /**
     * Sends an async GET request to the server to retrieve info about this session.
     */
    private void getInfoFromServer(){
        Callback callback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
                Log.d(LOG_TAG , "Http GET for Session failed!");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    if (response.code() == 200) {
                        String body = response.body().string();
                        Log.d(LOG_TAG, body);

                        JSONObject object = new JSONObject(body);

                        parseJSONObject(object);
                    } else if (response.code() == 404) {
                        Log.d(LOG_TAG, "Session not found!");
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
        Log.d(LOG_TAG, "Sending GET about session.");
        client.get(NetworkUtilities.HTTP_URL + "/session/" + sessionID, callback);
    }

    private void parseJSONObject(JSONObject object) throws JSONException{

        try {

            if (object.has("sessionState") && !isMaster())
                sessionState = object.getString("sessionState");

            if (object.has("master")) {
                master = object.getString("master");
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

                if (user.getUUID().compareTo(UUID.fromString(master)) == 0) {
                    isMaster = true;

                    // If we are the master and are getting updates, start u
                    if (Broadcaster.getInstance() == null) {
                        Broadcaster broadcaster = new Broadcaster(this);
                        broadcaster.addTransmitter(new ServerTransmitter(this));
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
                            broadcaster.addTransmitter(new ServerTransmitter(this));
                    }

                } else {
                    if (Listener.getInstance() == null) {
                        Listener listener = new Listener(this);
                        listener.addReceiver(new ServerReceiver(listener));
                    }
                }

                Log.d(LOG_TAG , "isMaster is : " + isMaster + "with master" + master);
            }

            if (object.has("users")) {
                JSONArray array = object.getJSONArray("users");
                members.update(array);
            }

            if (object.has("songs") && object.has("queue")) {
                JSONArray songArray = object.getJSONArray("songs");
                JSONArray queueArray = object.getJSONArray("queue");
                songQueue.update(songArray, queueArray);
            }

            // TODO : ensure that this works in sync with Listener.
            // TODO : figure out how to know when to update the Master's audiosessionstate, and when to
            // update the server instead
            if (object.has("sessionState")) {
                // TODO : re-enable this when we're ready, then test and implement it seperately

            }

            if (object.has("songID")) {
                newSongID = object.getInt("songID");
            }

            Log.d(LOG_TAG, "Parsing complete");
            isLoaded = true;
        } catch (Exception e){
            e.printStackTrace();
            Log.d(LOG_TAG, "Unknown Exception in UnisongSession.parseJSON!");
        }
    }

    /**
     * Creates the session on the server side.
     */
    private void create(){
        getCreateThread().start();
    }

    /**
     * Configures the SocketIOClient with the event listeners required to keep us up to date with
     * the session information
     */
    public void configureSocketIO(){
        if(socketIOConfigured)
            return;

        // NOTE : be sure to keep this up to doate with disconnectSocketIO()
        socketIOConfigured = true;
        socketIOClient.on("user joined", userJoined);
        socketIOClient.on("update session", updateSessionListener);
        socketIOClient.on("get session", getSessionListener);
        socketIOClient.on("user left", userLeft);
        socketIOClient.on("end session", endSessionListener);
        socketIOClient.on("kick", kickListener);
        socketIOClient.on("kick result", kickResultListener);
        getUpdate();
        Log.d(LOG_TAG, "Configured Socket.IO");
    }

    /**
     * Removes all listeners for this session from socketIOClient
     */
    public void disconnectSocketIO(){
        if(!socketIOConfigured)
            return;

        socketIOConfigured = false;
        socketIOClient.emit("leave", getSessionID());
        socketIOClient.off("user joined", userJoined);
        socketIOClient.off("update session", updateSessionListener);
        socketIOClient.off("get session", getSessionListener);
        socketIOClient.off("user left", userLeft);
        socketIOClient.off("end session", endSessionListener);
        socketIOClient.off("kick", kickListener);
        socketIOClient.off("kick result", kickResultListener);
        Log.d(LOG_TAG , "Socket.IO disconnected for session #" + sessionID);
    }

    public int incrementNewSongID(){
        int oldSongID = newSongID;
        newSongID++;
        return oldSongID;
    }


    private Thread getCreateThread(){
        return new Thread(() ->{
            Response response;
            Log.d(LOG_TAG , "Creating Unisong session.");
            try {
                response = client.syncPost(NetworkUtilities.HTTP_URL + "/session/", new JSONObject());

                if(response.code() == 200){
                    String body = response.body().string();
                    JSONObject object = new JSONObject(body);
                    Log.d(LOG_TAG , object.toString());
                    sessionID = object.getInt("sessionID");
                    Log.d(LOG_TAG , "Session ID : " + sessionID);

                    socketIOClient.joinSession(sessionID);
                    members.add(CurrentUser.getInstance());
                    isLoaded = true;
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
                socketIOClient.emit("authenticate", credentials);
            } catch (JSONException e){
                e.printStackTrace();
            }

        });

    }

    public int getCurrentSongID(){
        return songQueue.getCurrentSong().getID();
    }

    public Song getCurrentSong(){
        if(songQueue != null)
            return songQueue.getCurrentSong();

        return null;
    }

    public void startSong(int songID){
        if(isMaster){
            Log.d(LOG_TAG , "Sending start song.");
            Broadcaster.getInstance().startSong(songQueue.getSong(songID));
        }
    }

    public void endSession(){

    }

    public void destroy(){
        if(isMaster)
            socketIOClient.emit("end session" , getSessionID());

        songQueue = null;
        isLoaded = false;
        clients = null;
    }

    public void addFrame(AudioFrame frame){
        Song song = songQueue.getSong(frame.getSongID());
        // TODO : store frames without a song in case we miss a create song
        if(song != null)
            song.addFrame(frame);

    }

    public SessionMembers getMembers(){
        return members;
    }

    public SongQueue getSongQueue(){return songQueue;}

    public int getSessionID(){
        return sessionID;
    }

    public void addSong(Song song){
        songQueue.addSong(song);


        if(songQueue.size() == 1 && isMaster){
            Log.d(LOG_TAG, "The first song has been added, automatically playing");

            // TODO : investigate if the first song should auto-play
            //startSong(song.getID());
        }

        Log.d(LOG_TAG, "Creating song on server");

        // If we are the master, then notify the server. If not, then we are simply responding.
        if(isMaster())
            socketIOClient.emit("add song" , song.toJSON());
    }

    public String getSessionState(){
        return sessionState;
    }

    /**
     * Remove a song from the SongSession, with an ID. calls "delete song" in the socket.io client
     * if we are the session master
     * @param ID - the ID of the  given song
     */
    public void deleteSong(int ID){
        songQueue.deleteSong(ID);

        Object[] args = new Object[2];
        args[0] = ID;
        args[1] = sessionID;

        if(isMaster())
            socketIOClient.emit("delete song" , args);
    }

    /**
     * The Listener for socket.io that listens for session update events.
     */
    private Emitter.Listener updateSessionListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                JSONObject object = (JSONObject) args[0];
                parseJSONObject(object);
                Log.d(LOG_TAG  , "update session Received");
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    /**
     * Listens for returns to the "get session" event to update the session state on the client.
     */
    private Emitter.Listener getSessionListener = (Object... args) -> {
        try {
            JSONObject object = (JSONObject) args[0];
            parseJSONObject(object);
            Log.d(LOG_TAG, "get session Received");
            if (swipeRefresh != null&& refreshActivity != null) {
                refreshActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefresh.setRefreshing(false);
                        swipeRefresh = null;
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    };


    /**
     * Updates the session with information from the server.
     */
    public void getUpdate(){
        if(System.currentTimeMillis() - lastUpdate >= 100) {
            lastUpdate = System.currentTimeMillis();
            Log.d(LOG_TAG , "Updating UnisongSession");
            socketIOClient.emit("get session", getSessionID());
        }
    }

    public void sendUpdate(){
        Object[] array = new Object[2];
        array[0] = sessionID;
        array[1] = this.toJSON();
        socketIOClient.emit("update session", array);

        socketIOClient.emit("update songs", songQueue.getSongsJSON());
    }

    public boolean isMaster(){
        return isMaster;
    }

    /**
     * Leaves the current session. If this user is the master, the session is destroyed.
     *
     * The following steps are taken.
     * If Master:
     * The broadcaster is destroyed.
     *
     * If not:
     * The listener is destroyed
     *
     * Always :
     * The socket.io listeners are disconnected, "leave" is emitted,
     * SongQueue is disconnected from the AudioStatePublisher
     * The current session is set to null
     *
     * If we are currently playing, an END_SONG is emitted.
     *
     * The publisher is cleared.
     * destroy() is called.
     */
    public void leave(){
        isLoaded = false;
        if(isMaster()){

            Broadcaster broadcaster = Broadcaster.getInstance();

            if(broadcaster != null)
                broadcaster.destroy();


        } else {
            Listener listener = Listener.getInstance();
            if(listener != null)
                listener.destroy();

        }

        SessionUtils.removeSession(getSessionID());

        // This should always be true
        if(this == currentSession) {
            disconnectSocketIO();
            setCurrentSession(null);
            AudioStatePublisher publisher = AudioStatePublisher.getInstance();
            publisher.detach(songQueue);

            if (publisher.getState() == AudioStatePublisher.PLAYING)
                publisher.endSong(getCurrentSongID());


            publisher.clear();
        } else {
            Log.d(LOG_TAG , "Leaving session that is not current session!");
        }

        destroy();
    }

    /**
     * Broadcasts a 'kick' event to the Socket.IO server to kick the
     * selected user. Will only work if the current user is the session master
     * and authenticated
     * @param user
     */
    public void kick(User user){
        members.remove(user);
        socketIOClient.emit("kick", user.getUUID().toString());
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
    private Emitter.Listener kickResultListener = (Object[] args) -> {
        try{
            JSONObject response = (JSONObject) args[0];

            if(response.getInt("code") == 200){
                Log.d(LOG_TAG, "Kick successful! Response received");

                String uuid = response.getString("response");

                User user = UserUtils.getUser(uuid);

                Log.d(LOG_TAG , user.toString() + " to be kicked");
                members.remove(user);

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
    private Emitter.Listener kickListener = (Object[] args) -> {
        try{
            String uuid = (String) args[0];

            User user = UserUtils.getUser(uuid);

            Log.d(LOG_TAG , "Kick received for user : " + user);

            if(members.contains(user)){
                members.remove(user);
            }

            if(user.equals(CurrentUser.getInstance())){
                leave();
                // TODO : remove us from the session and move us back to the main screen
                // to do this we'll need a broadcast receiver in the invite friends activity and MainSessionActivty
                if(sessionHandler != null) {
                    Message message = new Message();
                    message.what = MainSessionActivity.KICKED;
                    sessionHandler.sendMessage(message);
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

            for (User user : members.getList()) {
                users.put(user.getUUID().toString());
            }


            object.put("users", users);

            object.put("queue", songQueue.getJSONQueue());
            object.put("songs", songQueue.getSongsJSON());

            object.put("master" , master);

            if(currentSession == this){

                AudioStatePublisher publisher = AudioStatePublisher.getInstance();

                if(publisher.getState() == AudioStatePublisher.IDLE){
                    object.put("sessionState" , "idle");
                } else if(publisher.getState()  == AudioStatePublisher.PAUSED){
                    object.put("sessionState" , "idle");
                } else if(publisher.getState() == AudioStatePublisher.PLAYING){
                    object.put("sessionState" , "playing");
                }
            } else {
                object.put("sessionState" , sessionState);
            }

        } catch (JSONException e){
            e.printStackTrace();
        }

        return object;
    }


    // The listener for when a user joins a session
    private Emitter.Listener userJoined = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "User joined received");
            try {
                String UUID = (String) args[0];

                User user = UserUtils.getUser(java.util.UUID.fromString(UUID));

                members.add(user);
                Log.d(LOG_TAG, "User " + user.toString() + "  joined session!");
            } catch (ClassCastException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "Format error in 'user joined'");
            }
        }
    };

    // The listener for when a user leaves a session
    private Emitter.Listener userLeft = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "user left received");
            try {
                String UUID = (String) args[0];

                User user = UserUtils.getUser(java.util.UUID.fromString(UUID));

                members.remove(user);
            } catch (ClassCastException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "Format error in 'user joined'");
            }
        }
    };


    // The listener for when a user leaves a session
    private Emitter.Listener endSessionListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                int sessionID = (Integer) args[0];

                if(UnisongSession.this.sessionID == sessionID && !isMaster())
                    endSession();


                if(sessionHandler != null) {
                    Message message = new Message();
                    message.what = MainSessionActivity.END_SESSION;
                    sessionHandler.sendMessage(message);
                }

            } catch (ClassCastException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "Format error in 'end session'");
            }
        }
    };


    public void updateSong(JSONObject object){
        songQueue.updateSong(object);
    }

    public void setSessionActivityHandler(MainSessionActivity.SessionMessageHandler handler){
        sessionHandler = handler;
    }

    public void setRefreshLayout(FragmentActivity activity, SwipeRefreshLayout swipeRefresh){
        this.refreshActivity = activity;
        this.swipeRefresh = swipeRefresh;
    }


    /**
     * If the session is currently playing, then play the song
     */
    public void playIfPlaying(){
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                startPlaying();
            }
        }, 5000);
    }

    private void startPlaying(){

        if(this != currentSession || !isLoaded || sessionState == null || !sessionState.equals("playing")) {
            Log.d(LOG_TAG , "Not updating session status.");
            return;
        }


        try {
            Log.d(LOG_TAG, "Playing the session with a seek");
            TimeManager timeManager = TimeManager.getInstance();
            Song currentSong = getCurrentSong();
            long startTime = currentSong.getSongStartTime();

            if (startTime == 0)
                return;

            timeManager.setSongStartTime(startTime);

            if(timeManager.getSongTime() > currentSong.getDuration()){
                timeManager.setSongStartTime(0);
                return;
            }

            if(!isMaster) {
                // TODO : replace the default 500 frames.
                Listener listener = Listener.getInstance();
                listener.requestData(getCurrentSong());
            }

            publisher.setState(AudioStatePublisher.PLAYING);
            Log.d(LOG_TAG , "Seeking to time " + timeManager.getSongTime());
            publisher.seek(timeManager.getSongTime());

        } catch (NullPointerException e){
            e.printStackTrace();
        }
    }

}