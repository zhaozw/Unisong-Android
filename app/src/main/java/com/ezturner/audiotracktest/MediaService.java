package com.ezturner.audiotracktest;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;

import com.ezturner.audiotracktest.audio.AudioTrackManager;
import com.ezturner.audiotracktest.network.AudioBroadcaster;
import com.ezturner.audiotracktest.network.AudioListener;
import com.ezturner.audiotracktest.network.Master;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Ethan on 1/25/2015.
 */
public class MediaService extends Service{

    private IBinder mBinder = new MediaServiceBinder();

    private AudioListener mListener;
    private AudioBroadcaster mBroadcaster;
    private AudioTrackManager mAudioTrackManager;

    private static boolean sIsMulticast = false;
    private boolean mIsPlaying;
    private boolean mIsPaused;

    private static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";


    public MediaService(){
        mIsPaused = false;
        mIsPlaying = true;

    }

    public void togglePlay(){

    }

    public void playFromMaster(Master master){
        mListener.playFromMaster(master);
    }

    public boolean isPlaying(){
        return mIsPlaying;
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
