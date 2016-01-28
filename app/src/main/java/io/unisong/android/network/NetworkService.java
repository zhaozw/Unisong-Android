package io.unisong.android.network;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.facebook.FacebookSdk;

import io.unisong.android.network.client.Listener;
import io.unisong.android.network.host.Broadcaster;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.ntp.TimeManager;

/**
 * Created by Ethan on 12/31/2015.
 * This service will start up the NTP processes, the HTTPClient, the socket.io client, and the general network communications.
 * It will also load up the user information and friends list.
 */
public class NetworkService extends Service {

    private final static String LOG_TAG = NetworkService.class.getSimpleName();
    private IBinder mBinder = new NetworkServiceBinder();

    private Listener listener;
    private Broadcaster broadcaster;
    private DiscoveryHandler discoveryHandler;
    private HttpClient client;
    private SocketIOClient mSocketIO;

//    private UtpServer mUtpServer = new UtpServer();
//    private UtpClient mUtpClient = new UtpClient();

    private TimeManager mTimeManager;

    @Override
    public void onCreate(){
        FacebookSdk.sdkInitialize(getApplicationContext());


        Log.d(LOG_TAG, "Creating NetworkService");

        client = HttpClient.getInstance();
        if(client == null) {
            client = new HttpClient(getApplicationContext());
            client.checkIfLoggedIn();
        }

        // TODO : only keep us connected when A: the user is using the app
        // TODO : and B: a user is logged in
        mSocketIO = new SocketIOClient(getApplicationContext());

        mTimeManager = new TimeManager();

        ConnectionUtils utils = new ConnectionUtils();
        ConnectionUtils.setInstance(utils);

        discoveryHandler = new DiscoveryHandler();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class NetworkServiceBinder extends Binder {
        /**
         * Returns the instance of this service for a client to make method calls on it.
         * @return the instance of this service.
         */
        public NetworkService getService() {
            return NetworkService.this;
        }

    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        if(broadcaster != null){
            broadcaster.destroy();
            broadcaster = null;
        }

        if(listener != null){
            listener.destroy();
            listener = null;
        }

        if(discoveryHandler != null){
            discoveryHandler.destroy();
            discoveryHandler = null;
        }
        mTimeManager = null;
    }
}
