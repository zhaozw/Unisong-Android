package io.unisong.android.network;

import android.os.Handler;

/**
 * Created by Ethan on 10/1/2015.
 */
public class ConnectionUtils {

    private Handler mHandler;

    public ConnectionUtils(){
        mHandler = new Handler();
    }

    public void setActiveSessionsAdapter(){

    }

    private Runnable mCheckForHotspotsRunnable = new Runnable() {
        @Override
        public void run() {
            checkForHotspots();
            mHandler.postDelayed(mCheckForHotspotsRunnable , 500);
        }
    };

    /**
     * This method checks for Unisong hotspots.
     */
    private void checkForHotspots(){

    }
}
