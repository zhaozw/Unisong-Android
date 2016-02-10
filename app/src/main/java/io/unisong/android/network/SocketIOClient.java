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

    private static SocketIOClient instance;
    public static SocketIOClient getInstance(){
        return instance;
    }
    private final String LOG_TAG = SocketIOClient.class.getSimpleName();

    private Handler inviteHandler;
    private HttpClient httpClient;
    private Socket socket;
    private ServerReceiver mReceiver;
    private Context mContext;
    private boolean connected;

    public SocketIOClient(Context context){
        mContext = context;
        Log.d(LOG_TAG, "Starting SocketIO Client");
        httpClient = HttpClient.getInstance();

        IO.Options opts = new IO.Options();
        opts.forceNew = true;

        inviteHandler = new Handler();
        socket = IO.socket(NetworkUtilities.getSocketIOUri() , opts);

        instance = this;
    }

    public void setServerReceiver(ServerReceiver receiver){
        mReceiver = receiver;
    }

    public void connect(){
        if(!connected) {
            Log.d(LOG_TAG, "Connecting to server with socket.io");
            socket.connect();
            socket.on(Socket.EVENT_CONNECT, mConnectListener);
            socket.on(Socket.EVENT_RECONNECT, mReconnectListener);
            socket.on(Socket.EVENT_DISCONNECT, disconnectListener);
            // TODO : see if there's a better place to put this
            socket.on("invite user", inviteListener);
            socket.on("join session result", joinResultListener);
            socket.on("authentication result", authenticationResult);
            connected = true;
        }
    }

    public boolean isConnected(){
        return socket.connected();
    }

    public void on(String eventName, Emitter.Listener listener){
        socket.on(eventName, listener);
    }

    public void off(String eventName, Emitter.Listener listener){
        socket.off(eventName, listener);
    }

    public void joinSession(int sessionID){
        if(socket.connected())
            socket.emit("join session", sessionID);
    }

    private Runnable loginRunnable = () -> login();

    /**
     * This method will authenticate the user over the socket if the socket is connected.
     *
     */
    private void login(){

        User user = CurrentUser.getInstance();

        if(user == null || user.getUsername() == null) {
            inviteHandler.postDelayed(loginRunnable, 2000l);
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
            socket.emit("authenticate", object);
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
            inviteHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    inviteHandler.removeCallbacks(loginRunnable);
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
    private Emitter.Listener disconnectListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            for(Object object : args){
                Log.d(LOG_TAG , "Object : " + object.toString());
            }
            Log.d(LOG_TAG , "Disconnected.");
        }

    };

    private Emitter.Listener inviteListener = new Emitter.Listener() {
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

                if(inviteHandler != null)
                    inviteHandler.sendMessage(message);

            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    public void emit(String eventName, Object data){
        if(socket.connected())
            socket.emit(eventName , data);
    }

    public void emit(String eventName, Object[] args){
        if(socket.connected())
            socket.emit(eventName, args);
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
        while(httpClient == null){
            httpClient = HttpClient.getInstance();
        }
        // wait while we're not logged in
        while(!httpClient.isLoggedIn()){

        }
    }

    // Destroys the object removes all references
    public void destroy(){
        socket.disconnect();
        mContext = null;
        instance = null;
    }

    private Emitter.Listener joinResultListener = (Object[] args) -> {
        try{
            JSONObject object = (JSONObject) args[0];

            Log.d(LOG_TAG , "Join Result Received: " + object.toString());
        } catch (ClassCastException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "Casting failed in JoinResultListener");
        }
    };

    public void registerInviteHandler(UnisongActivity.IncomingHandler handler){
        inviteHandler = handler;
    }

    private Emitter.Listener authenticationResult = new Emitter.Listener() {
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
