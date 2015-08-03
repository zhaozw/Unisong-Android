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

import com.ezturner.speakersync.audio.AudioStatePublisher;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.slave.SlaveDecoder;
import com.ezturner.speakersync.network.TimeManager;
import com.ezturner.speakersync.network.client.ListenerBridge;
import com.ezturner.speakersync.network.master.Broadcaster;
import com.ezturner.speakersync.network.master.MasterDiscoveryHandler;
import com.ezturner.speakersync.network.ntp.SntpClient;
import com.ezturner.speakersync.network.client.AudioListener;

/**
 * Created by Ethan on 1/25/2015.
 */
public class MediaService extends Service{

    static final String LOG_TAG = MediaService.class.getSimpleName();

    private IBinder mBinder = new MediaServiceBinder();

    private AudioListener mListener;
    private Broadcaster mBroadcaster;

    private BroadcastReceiver mMessageReceiver ;
    private SntpClient mSntpClient;

    public static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";
    public static final String LARGE_TEST_FILE_PATH = "/storage/emulated/0/music/1hour.mp3";

    public PowerManager.WakeLock mWakeLock;

    private TimeManager mTimeManager;

    private AudioStatePublisher mAudioStatePublisher;

    private AudioTrackManager mAudioTrackManager;

    //The time that we have paused at, relative to the Song start time.
    private long mResumeTime = 0;



    public void startToListen(){
        //TODO: uncomment after compile
        //mListener = new AudioListener(this);
    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d(LOG_TAG, "Starting MediaService");

        mSntpClient = new SntpClient();

        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent

                String command = intent.getStringExtra("command");

                if(command.equals("listener")){
                    Log.d(LOG_TAG , "Listener received!");
                    listener();
                } else if(command.equals("broadcaster")){
                    Log.d(LOG_TAG , "Broadcaster received!");
                    broadcaster();
                }else if(command.equals("play")){
                    Log.d(LOG_TAG , "Play received!");
                    play();
                }else if(command.equals("pause")){
                    Log.d(LOG_TAG , "Pause received!");
                    pause();
                }else if(command.equals("resume")){
                    Log.d(LOG_TAG , "Resume received!");
                    resume();
                }else if(command.equals("seek")){
                    Log.d(LOG_TAG , "Seek received!");
                    seek();
                }  else if(command.equals("destroy")){
                    Log.d(LOG_TAG , "Destroy received!");
                    onDestroy();
                }

            }
        };

        //Register the broadcast reciever
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("service-interface"));

        mAudioStatePublisher = AudioStatePublisher.getInstance();

        mTimeManager = new TimeManager(SntpClient.getInstance());
        //The instanatiation of AudioTrackManager needs to be after that of TimeManager!
        mAudioTrackManager = new AudioTrackManager();


        PowerManager mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mgr.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MyWakeLock");
        mWakeLock.acquire();
    }

    public void broadcaster() {
        if(mBroadcaster == null) {
            mBroadcaster = new Broadcaster();
        }
    }


    public void listener(){
        mListener = new AudioListener();
    }

    public void play(){

    }

    public void pause(){
        mAudioStatePublisher.update(AudioStatePublisher.PAUSED);
    }

    public void resume(){
        mAudioStatePublisher.update(AudioStatePublisher.RESUME);
    }

    public void seek() {
        mAudioStatePublisher.seek(100000);
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

        if(mBroadcaster != null){
            mBroadcaster.destroy();
            mBroadcaster = null;
        }

        if(mListener != null){
            mListener.destroy();
            mListener = null;
        }

        Log.d(LOG_TAG , "Destroying mAudioTrackManager");


        if(mSntpClient != null){
            mSntpClient.destroy();
            mSntpClient = null;
        }

        mTimeManager = null;

        System.gc();
    }

}
