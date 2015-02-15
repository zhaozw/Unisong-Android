package com.ezturner.speakersync;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.network.AudioBroadcaster;
import com.ezturner.speakersync.network.AudioListener;
import com.ezturner.speakersync.network.Master;

/**
 * Created by Ethan on 1/25/2015.
 */
public class MediaService extends Service{

    private IBinder mBinder = new MediaServiceBinder();

    private AudioListener mListener;
    private AudioBroadcaster mBroadcaster;
    private AudioTrackManager mAudioTrackManager;

    //Objects for enabling multicast
    private static WifiManager wifiManager;
    private static WifiManager.MulticastLock mCastLock;


    //The phone's phone number
    private static String sPhoneNumber;


    private static boolean sIsMulticast = false;

    private static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";


    public MediaService(){

        //Start the Multicast manager objects
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        mCastLock = wifiManager.createMulticastLock("mydebuginfo");

        //Get the phone number
        TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        sPhoneNumber = tMgr.getLine1Number();

        //TODO: Set a placeholder number for when the above method doesn't work
        if(sPhoneNumber == null){
            sPhoneNumber = "";
        }
    }

    public void startToListen(){
        //TODO: uncomment after compile
        //mListener = new AudioListener(this);
    }

    public void togglePlay(){
        if(MyApplication.isPlaying()){
            //pause
        } else{
            //play
        }

    }

    public void playFromMaster(Master master){
        mListener.playFromMaster(master);
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

    public static boolean multicastLockIsHeld(){
        return mCastLock.isHeld();
    }

    public static void aquireMulticastLock(){
        if(!multicastLockIsHeld())
            mCastLock.acquire();
    }

    public static void releaseMulticastLock(){
        if(multicastLockIsHeld())
            mCastLock.release();
    }


    public static String getPhoneNumber(){
        return sPhoneNumber;
    }


}
