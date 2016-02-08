package io.unisong.android.network.connection;

import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.unisong.android.R;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;

/**
 * This class should be able to tell the following, and propagate changes to its constituent objects:
 *
 * -If we are currently unauthorized(cookie expire/non-existent) and need to reauthorize (should trigger reauth)
 * -If the phone is on Airplane mode
 * -If the servers are down
 * -If we have the wrong API version
 *
 * Created by Ethan on 2/7/2016.
 */
public class ConnectionStatePublisher {

    private static ConnectionStatePublisher instance;

    public static ConnectionStatePublisher getInstance(){
        return instance;
    }

    private static final String LOG_TAG = ConnectionStatePublisher.class.getSimpleName();

    public static final int ONLINE = 0;
    public static final int SERVER_OFFLINE = 1;
    public static final int WRONG_API = 2;
    public static final int INTERNET_DOWN = 3;
    private final String apiVersion;

    private int state;
    private List<ConnectionObserver> observers;
    private HttpClient client;

    public ConnectionStatePublisher(Context context){
        state = ONLINE;
        observers = new ArrayList<>();

        apiVersion = context.getResources().getString(R.string.rest_api_version);
        instance = this;
    }

    public void setHttpClient(HttpClient client){
        this.client = client;
        getApiVersion();
    }

    private Callback getApiVersionCallback = new Callback() {
        @Override
        public void onFailure(Request request, IOException e) {
            // TODO : determine which exceptions are thrown where. This is an excellent opportunity
            // to notify an user early if we cannot connect or airplane mode is on
        }

        @Override
        public void onResponse(Response response) throws IOException {
            if(response.code() == 200){

                String serverApiVersion = response.body().string();

                Log.d(LOG_TAG, "Server API version is : " + serverApiVersion);

                if(!apiVersion.equals(serverApiVersion)){
                    update(WRONG_API);
                }
            } else if(response.code() == 404){
                update(WRONG_API);
            }
        }
    };

    public void getApiVersion(){
        client.get(NetworkUtilities.HTTP_URL + "/version" , getApiVersionCallback);
    }

    public void update(int state){
        synchronized (observers) {
            this.state = state;

            for (ConnectionObserver observer : observers) {
                observer.updateConnectionState(state);
            }
        }
    }

    public void attach(ConnectionObserver observer){
        synchronized (observers) {
            observers.add(observer);
        }
        observer.updateConnectionState(state);
    }

    public void detach(ConnectionObserver observer){
        synchronized (observers) {
            observers.remove(observer);
        }
    }

    public void serverOffline(){
        update(SERVER_OFFLINE);
    }

    public void connectionOffline(){
        update(INTERNET_DOWN);
    }

    public void wrongAPI(){
        update(WRONG_API);
    }

    public void destroy(){
        synchronized (observers){
            observers = new ArrayList<>();
        }
    }
}
