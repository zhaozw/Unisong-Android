package io.unisong.android.network;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.unisong.android.audio.AudioFrame;

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

    public SocketIOClient(){

        mHttpClient = HttpClient.getInstance();

        while(mHttpClient.getCookieManager().getCookieStore().getCookies().size() < 0) {
            synchronized (this){
                try{
                    this.wait(50);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
        try {
            IO.Options options = new IO.Options();

            //TODO: figure out how to get a specific cookie.
            Log.d(LOG_TAG, "Cookies : " + mHttpClient.getCookieManager().getCookieStore().getCookies().size());
            options.query = mHttpClient.getCookieManager().getCookieStore()
                    .get(new URI(NetworkUtilities.EC2_INSTANCE)).get(0).toString();
            mSocket = IO.socket(NetworkUtilities.SOCKETIO_URL);
//        mSocket.
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

//        mSocket.on(Socket.EVENT_DISCONNECT, );

        mSocket.on("data" , mDataListener);
        connect();
        mIsLoggedIn = false;
    }

    public void connect(){
        mSocket.connect();
    }

    public boolean isConnected(){
        return mSocket.connected();
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
     * The listener for when we get data.
     *
     */
    private Emitter.Listener mDataListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "Data event received.");
            for(int i = 0; i < args.length; i++){
                Log.d(LOG_TAG , args[i].toString());
            }
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

    public void upload(AudioFrame frame){
        JSONObject obj = new JSONObject();
        try {
            obj.put("dataid", frame.getID());
            obj.put("data", frame.getData());
            obj.put("songid", 5);
        } catch (JSONException e){
            e.printStackTrace();
        }

        mSocket.emit("upload data", obj);


    }
}
