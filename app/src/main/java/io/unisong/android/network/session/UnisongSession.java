package io.unisong.android.network.session;

import android.util.Log;

import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.unisong.android.network.Client;
import io.unisong.android.network.Host;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.Song;
import io.unisong.android.network.http.HttpClient;
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

    public UnisongSession(){

        mClient = HttpClient.getInstance();
        mSocketIOClient = SocketIOClient.getInstance();

        mSongQueue = new SongQueue();
        mMembers = new ArrayList<>();
        mIsMaster = true;

        create();
        configureSocketIO();
        mIsDisconnected = false;
        sInstance = this;
    }

    public UnisongSession(String sessionID){

    }

    /**
     * Creates the session on the server side.
     */
    private void create(){
        getCreateThread().start();
    }

    private void configureSocketIO(){

    }


    private Thread getCreateThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                Response response;
                try {
                    response = mClient.post(NetworkUtilities.HTTP_URL + "/session/", new JSONObject());

                    if(response.code() == 200){
                        String body = response.body().string();
                        JSONObject object = new JSONObject(body);
                        mSessionID = object.getInt("sessionID") + "";
                    }
                } catch (IOException e){
                    e.printStackTrace();
                    return;
                } catch (JSONException e){
                    Log.d(LOG_TAG, "JSON Parsing failed.");
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

    public List<User> getMembers(){
        return mMembers;
    }

}
