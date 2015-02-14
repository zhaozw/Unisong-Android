package com.ezturner.audiotracktest;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Ethan on 1/25/2015.
 */
public class MediaService extends Service{

    private IBinder mBinder = new MediaServiceBinder();

    private static boolean sIsMulticast = false;
    private boolean mIsPlaying;
    private boolean mIsPaused;

    private static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";


    public MediaService(){


    }

    public void togglePlay(){

    }

    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class MediaServiceBinder extends Binder {
        /**
         * Returns the instance of this service for a client to make method calls on it.
         * @return the instance of this service.
         */
        public MediaService getService() {
            return MediaService.this;
        }

    }

    public static boolean isMulticast(){
        return sIsMulticast;
    }




}
