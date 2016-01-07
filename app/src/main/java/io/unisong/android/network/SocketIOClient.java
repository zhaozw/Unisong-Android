package io.unisong.android.network;

import android.os.Handler;
import android.util.Log;


import com.facebook.AccessToken;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.unisong.android.network.client.receiver.ServerReceiver;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;

/**
 * This class handles all communication between the android client
 * and the server with Socket.io
 *
 * Created by Ethan on 9/5/2015.
 */
public class SocketIOClient {

    private static SocketIOClient sInstance;
    public static SocketIOClient getInstance(){
        if(sInstance == null){
            sInstance = new SocketIOClient();
        }
        return sInstance;
    }
    private final String LOG_TAG = SocketIOClient.class.getSimpleName();

    private Handler mHandler;
    private HttpClient mHttpClient;
    private Socket mSocket;
    private ServerReceiver mReceiver;

    public SocketIOClient(){
        Log.d(LOG_TAG, "Starting SocketIO Client");
        mHttpClient = HttpClient.getInstance();

        IO.Options opts = new IO.Options();
        opts.forceNew = true;

        mHandler = new Handler();
        mSocket = IO.socket(NetworkUtilities.getSocketIOUri() , opts);

        connect();
    }

    public void setServerReceiver(ServerReceiver receiver){
        mReceiver = receiver;
    }

    public void connect(){
        Log.d(LOG_TAG, "Connecting to server with socket.io");
        mSocket.connect();
        mSocket.on(Socket.EVENT_CONNECT , mConnectListener);
        mSocket.on(Socket.EVENT_RECONNECT, mReconnectListener);
        mSocket.on(Socket.EVENT_DISCONNECT , mDisconnectListener);

    }

    public boolean isConnected(){
        return mSocket.connected();
    }

    public void on(String eventName, Emitter.Listener listener){
        mSocket.on(eventName , listener);
    }

    public void joinSession(int sessionID){
        mSocket.emit("join session", sessionID);
    }

    private Runnable mLoginRunnable = () -> login();

    /**
     * This method will authenticate the user over the socket if the socket is connected.
     *
     */
    private void login(){

        User user = CurrentUser.getInstance();

        if(user == null || user.getUsername() == null) {
            mHandler.postDelayed(mLoginRunnable, 2000l);
            return;
        }

        Log.d(LOG_TAG , "Logging in over socket.io");

        try {
            JSONObject object = new JSONObject();

            object.put("username", user.getUsername());
            // if this is a facebook user then authenticate with the facebook access token
            if (user.isFacebookUser()) {
                object.put("accessToken", AccessToken.getCurrentAccessToken().getToken());
            } else {
                // otherwise, use password
                object.put("password", user.getPassword());
            }
            mSocket.emit("authenticate", object);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * The listener for when the socket gets connected.
     *
     */
    private Emitter.Listener mConnectListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG, "Connection event.");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mHandler.removeCallbacks(mLoginRunnable);
                    login();

                    UnisongSession currentSession = UnisongSession.getCurrentSession();

                    if(currentSession != null){
                        joinSession(currentSession.getSessionID());
                        currentSession.getUpdate();
                    }

                }
            }, 250);
        }

    };

    /**
     * The listener for when the socket gets disconnected.
     *
     */
    private Emitter.Listener mReconnectListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "Reconnection event.");
        }

    };

    /**
     * The listener for when the socket gets disconnected.
     *
     */
    private Emitter.Listener mDisconnectListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            for(Object object : args){
                Log.d(LOG_TAG , "Object : " + object.toString());
            }
            Log.d(LOG_TAG , "Disconnected.");
        }

    };

    public void emit(String eventName, Object data){
        mSocket.emit(eventName , data);
    }

    private Thread getConnectionThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                checkForLogin();
                connect();
                login();
            }
        });
    }
    private void checkForLogin(){
        // wait while we're not logged in
        while(!mHttpClient.isLoggedIn()){

        }
    }
}
