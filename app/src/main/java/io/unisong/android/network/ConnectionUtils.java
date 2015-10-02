package io.unisong.android.network;

import android.os.Handler;

import io.unisong.android.activity.ActiveSessionsAdapter;

/**
 * Created by Ethan on 10/1/2015.
 */
public class ConnectionUtils {

    private static ConnectionUtils sInstance;

    public static ConnectionUtils getInstance(){
        return sInstance;
    }

    public static void setInstance(ConnectionUtils utils){
        sInstance = utils;
    }

    private Handler mHandler;
    private ActiveSessionsAdapter mAdapter;

    public ConnectionUtils(){
        mHandler = new Handler();
    }

    public void setActiveSessionsAdapter(ActiveSessionsAdapter adapter){
        mAdapter = adapter;
    }

    private Runnable mCheckForHotspotsRunnable = new Runnable() {
        @Override
        public void run() {
            checkForHotspots();
            checkConnectionType();
            mHandler.postDelayed(mCheckForHotspotsRunnable , 500);
        }
    };

    /**
     * This method checks for Unisong hotspots.
     */
    private void checkForHotspots(){

    }

    private void checkConnectionType(){

    }
}
