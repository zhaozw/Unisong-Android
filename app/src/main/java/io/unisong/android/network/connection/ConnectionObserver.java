package io.unisong.android.network.connection;

/**
 * This is the interface that Observers that observe the ConnectionState from the
 * singleton ConnectionStatePublisher should implement.
 *
 * It only has one method, for updating the observers
 * Created by Ethan on 2/7/2016.
 */
public interface ConnectionObserver {

    void updateConnectionState(int state);
}
