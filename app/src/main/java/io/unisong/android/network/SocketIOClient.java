package io.unisong.android.network;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.facebook.AccessToken;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.unisong.android.activity.UnisongActivity;
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
        return sInstance;
    }
    private final String LOG_TAG = SocketIOClient.class.getSimpleName();

    private Handler mInviteHandler;
    private HttpClient mHttpClient;
    private Socket mSocket;
    private ServerReceiver mReceiver;
    private Context mContext;

    public SocketIOClient(Context context){
        mContext = context;
        Log.d(LOG_TAG, "Starting SocketIO Client");
        mHttpClient = HttpClient.getInstance();

        IO.Options opts = new IO.Options();
        opts.forceNew = true;

        mInviteHandler = new Handler();
        mSocket = IO.socket(NetworkUtilities.getSocketIOUri() , opts);

        sInstance = this;
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
        // TODO : see if there's a better place to put this
        mSocket.on("invite user", mInviteListener);
        mSocket.on("join session result" , mJoinResultListener);
        mSocket.on("authentication result" , mAuthenticationResult);

    }

    public boolean isConnected(){
        return mSocket.connected();
    }

    public void on(String eventName, Emitter.Listener listener){
        mSocket.on(eventName , listener);
    }

    public void joinSession(int sessionID){
        if(mSocket.connected())
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
            mInviteHandler.postDelayed(mLoginRunnable, 2000l);
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
                Log.d(LOG_TAG , "User Password : " + user.getPassword());
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
            mInviteHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mInviteHandler.removeCallbacks(mLoginRunnable);
                    login();

                    UnisongSession currentSession = UnisongSession.getCurrentSession();


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

    private Emitter.Listener mInviteListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "invite user received");
            try {
                JSONObject object = (JSONObject) args[0];

                String inviteMessage = object.getString("message");
                int sessionID = object.getInt("sessionID");

                Message message = new Message();

                message.what = UnisongActivity.INVITE;
                message.obj = object;

                if(mInviteHandler != null)
                    mInviteHandler.sendMessage(message);

            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    public void emit(String eventName, Object data){
        if(mSocket.connected())
            mSocket.emit(eventName , data);
    }

    public void emit(String eventName, Object[] args){
        if(mSocket.connected())
            mSocket.emit(eventName, args);
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

    // Destroys the object removes all references
    public void destroy(){
        mContext = null;
        sInstance = null;
    }

    private Emitter.Listener mJoinResultListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "Join Result Received!");
        }

    };

    public void registerInviteHandler(UnisongActivity.IncomingHandler handler){
        mInviteHandler = handler;
    }

    private Emitter.Listener mAuthenticationResult = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            int code = -1;
            try{
                code = (Integer) args[0];
            } catch (ClassCastException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "ClassCastException thrown in Authentication Result!");
                return;
            }

            if (code == 200) {
                UnisongSession currentSession = UnisongSession.getCurrentSession();

                if (currentSession != null) {
                    joinSession(currentSession.getSessionID());
                    currentSession.getUpdate();
                }
            }else if(code == 401){
                Log.d(LOG_TAG , "Authentication failed!");
            }
        }
    };
}
