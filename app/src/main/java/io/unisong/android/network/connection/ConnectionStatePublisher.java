package io.unisong.android.network.connection;

import java.util.ArrayList;
import java.util.List;

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

    public static final int ONLINE = 0;
    public static final int SERVER_OFFLINE = 1;
    public static final int WRONG_API = 2;
    public static final int INTERNET_DOWN = 3;

    private int state;
    private List<ConnectionObserver> observers;


    public ConnectionStatePublisher(){
        state = ONLINE;
        observers = new ArrayList<>();

        instance = this;
    }

    public void update(int state){
        synchronized (observers) {
            this.state = state;

            for (ConnectionObserver observer : observers) {
                observer.update(state);
            }
        }
    }

    public void attach(ConnectionObserver observer){
        synchronized (observers) {
            observers.add(observer);
        }
        observer.update(state);
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
