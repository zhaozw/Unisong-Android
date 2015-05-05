package com.ezturner.speakersync;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ezturner.speakersync.audio.AudioFileReader;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.BroadcasterBridge;
import com.ezturner.speakersync.audio.SlaveDecoder;
import com.ezturner.speakersync.audio.TrackManagerBridge;
import com.ezturner.speakersync.network.master.AudioBroadcaster;
import com.ezturner.speakersync.network.master.MasterDiscoveryHandler;
import com.ezturner.speakersync.network.ntp.SntpClient;
import com.ezturner.speakersync.network.slave.AudioListener;
import com.ezturner.speakersync.network.slave.ListenerBridge;
import com.ezturner.speakersync.network.slave.NetworkInputStream;

import java.io.IOException;

/**
 * Created by Ethan on 1/25/2015.
 */
public class MediaService extends Service{

    static final String LOG_TAG = MediaService.class.getSimpleName();

    private IBinder mBinder = new MediaServiceBinder();

    private AudioListener mListener;
    private AudioBroadcaster mBroadcaster;
    private AudioTrackManager mAudioTrackManager;
    private AudioFileReader mFileReader;

    private BroadcastReceiver mMessageReceiver ;

    private SlaveDecoder mSlaveDecoder;
    private MasterDiscoveryHandler mDiscovery;

    //Objects for enabling multicast
    private static WifiManager wifiManager;
    private static WifiManager.MulticastLock mCastLock;

    private SntpClient mSntpClient;

    public static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";

    public PowerManager.WakeLock mWakeLock;



    public void startToListen(){
        //TODO: uncomment after compile
        //mListener = new AudioListener(this);
    }

    @Override
    public void onCreate(){
        super.onCreate();

        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent

                String command = intent.getStringExtra("command");

                if(command.equals("listener")){
                    Log.d("ezturner" , "Listener received!");
                    listener();
                } else if(command.equals("broadcaster")){
                    Log.d("ezturner" , "Broadcaster received!");
                    broadcaster();
                }else if(command.equals("play")){
                    Log.d("ezturner" , "Play received!");
                    play();
                } else if(command.equals("destroy")){
                    Log.d("ezturner" , "Destroy received!");
                    onDestroy();
                }

            }
        };

        //Register the broadcast reciever
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("service-interface"));

        mAudioTrackManager = new AudioTrackManager();
        mSntpClient = new SntpClient(mAudioTrackManager);

        mFileReader = new AudioFileReader(new TrackManagerBridge(mAudioTrackManager));



        PowerManager mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mgr.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MyWakeLock");
        mWakeLock.acquire();


    }



    public void togglePlay(){





    }

    public void broadcaster() {
        if(mBroadcaster == null) {
            mBroadcaster = new AudioBroadcaster(mAudioTrackManager , mFileReader , mSntpClient);
            mFileReader.setBroadcasterBridge(new BroadcasterBridge(mBroadcaster));
        }
    }


    public void listener(){

        ListenerBridge bridge = new ListenerBridge(null , mAudioTrackManager );
        mListener = new AudioListener(this , bridge , mSntpClient);


    }

    public void play(){

    }

    public IBinder onBind(Intent arg0){
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



    @Override
    public void onDestroy(){
        super.onDestroy();
        mWakeLock.release();
    }

}
