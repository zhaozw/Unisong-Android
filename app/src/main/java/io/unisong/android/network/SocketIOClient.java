package io.unisong.android.network;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.network.client.receiver.ServerReceiver;

/**
 * This class handles all communication between the android client
 * and the server with Socket.io
 *
 * Created by Ethan on 9/5/2015.
 */
public class SocketIOClient {

    private final String LOG_TAG = SocketIOClient.class.getSimpleName();

    private HttpClient mHttpClient;
    private Socket mSocket;
    private boolean mIsLoggedIn;
    private ServerReceiver mReceiver;

    public SocketIOClient(){

        mHttpClient = HttpClient.getInstance();

        mSocket = IO.socket(NetworkUtilities.getSocketIOUri());


        mSocket.on("pause" , mPauseListener);

        connect();
        mIsLoggedIn = false;
    }

    public void setServerReceiver(ServerReceiver receiver){
        mReceiver = receiver;
    }

    public void connect(){
        mSocket.connect();
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
     * The listener for when we get a pause event
     *
     */
    private Emitter.Listener mPauseListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
        }

    };


    /**
     * The listener for when the socket gets disconnected.
     *
     */
    private Emitter.Listener mDisconnectListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
        }

    };

    public void emit(String eventName, JSONObject data){
        mSocket.emit(eventName , data);
    }

}