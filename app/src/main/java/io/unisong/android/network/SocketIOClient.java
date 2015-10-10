package io.unisong.android.network;

import android.util.Log;


import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.unisong.android.PrefUtils;
import io.unisong.android.network.client.receiver.ServerReceiver;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.user.CurrentUser;

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

    private HttpClient mHttpClient;
    private Socket mSocket;
    private boolean mIsLoggedIn;
    private ServerReceiver mReceiver;

    public SocketIOClient(){

        Log.d(LOG_TAG, "Starting SocketIO Client");
        mHttpClient = HttpClient.getInstance();

        IO.Options opts = new IO.Options();
        opts.forceNew = true;

        mSocket = IO.socket(NetworkUtilities.getSocketIOUri() , opts);

        getConnectionThread().start();
        mIsLoggedIn = false;
    }

    public void setServerReceiver(ServerReceiver receiver){
        mReceiver = receiver;
    }

    public void connect(){
        Log.d(LOG_TAG, "Connecting to server with socket.io");
        mSocket.connect();
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
        mSocket.emit("join session" , sessionID);
    }

    /**
     * This method will authenticate the user over the socket if the socket is connected.
     *
     */
//    public boolean login(){
//
//    }


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
                Log.d(LOG_TAG , object.getClass().getSimpleName());
            }
            Log.d(LOG_TAG , "Disconnected.");
        }

    };

    public void emit(String eventName, JSONObject data){
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

    private void login(){

    }
}
