package io.unisong.android.network;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * This class handles all communication between the android client
 * and the server with Socket.io
 *
 * Created by Ethan on 9/5/2015.
 */
public class SocketIOClient {


    private HttpClient mHttpClient;
    private Socket mSocket;
    private boolean mIsLoggedIn;

    public SocketIOClient(){

        mHttpClient = HttpClient.getInstance();

        try{
            IO.Options options = new IO.Options();

            options.query = "connect.sid=" + mHttpClient.getCookies().get("connect.sid");
            mSocket = IO.socket(NetworkUtilities.SOCKETIO_URL);
            mSocket.
        } catch (URISyntaxException e){
            e.printStackTrace();
        }

        mSocket.on(Socket.EVENT_DISCONNECT, );

        mIsLoggedIn = false;
    }

    public void connect(){
        mSocket.connect();
    }

    public boolean isConnected(){
        return mSocket.connected();
    }

    /**
     * This method will authenticate the user over the socket if the socket is connected.
     *
     */
    public boolean login(){

    }


    /**
     * The listener for when the socket gets disconnected.
     *
     */
    private Emitter.Listener mDisconnectListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
        }

    };
}
