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

import com.ezturner.speakersync.audio.master.AudioFileReader;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.BroadcasterBridge;
import com.ezturner.speakersync.audio.slave.SlaveDecoder;
import com.ezturner.speakersync.audio.TrackManagerBridge;
import com.ezturner.speakersync.network.AnalyticsSuite;
import com.ezturner.speakersync.network.TimeManager;
import com.ezturner.speakersync.network.master.AudioBroadcaster;
import com.ezturner.speakersync.network.master.MasterDiscoveryHandler;
import com.ezturner.speakersync.network.ntp.SntpClient;
import com.ezturner.speakersync.network.slave.AudioListener;
import com.ezturner.speakersync.network.slave.ListenerBridge;

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

    private TimeManager mTimeManager;

    private AnalyticsSuite mAnalyticsSuite;

    //The time that we have paused at, relative to the Song start time.
    private long mResumeTime = 0;



    public void startToListen(){
        //TODO: uncomment after compile
        //mListener = new AudioListener(this);
    }

    @Override
    public void onCreate(){
        super.onCreate();

        Log.d(LOG_TAG , "Starting MediaService");

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

        mAnalyticsSuite = new AnalyticsSuite();
        mSntpClient = new SntpClient();
        mTimeManager = new TimeManager(mSntpClient);
        mAudioTrackManager = new AudioTrackManager(mTimeManager);



        PowerManager mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mgr.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MyWakeLock");
        mWakeLock.acquire();


    }



    public void togglePlay(){





    }

    public void broadcaster() {
        if(mBroadcaster == null) {
            mFileReader = new AudioFileReader(new TrackManagerBridge(mAudioTrackManager));
            mBroadcaster = new AudioBroadcaster(mAudioTrackManager , mFileReader , mTimeManager, mAnalyticsSuite);
            mFileReader.setBroadcasterBridge(new BroadcasterBridge(mBroadcaster));
        }
    }


    public void listener(){
        mSlaveDecoder = new SlaveDecoder(new TrackManagerBridge(mAudioTrackManager ), 2, mTimeManager);
        ListenerBridge bridge = new ListenerBridge(mSlaveDecoder , mAudioTrackManager );
        mListener = new AudioListener(this , bridge , mSlaveDecoder, mTimeManager , mAnalyticsSuite);


    }

    public void play(){

    }

    public void pause(){
        if(mBroadcaster != null) {
            mResumeTime = mAudioTrackManager.pause();
            mBroadcaster.pause();
        }
    }

    public void resume(){
        if(mBroadcaster != null) {
            mBroadcaster.resume(mResumeTime);
            mAudioTrackManager.resume(mResumeTime);
            mResumeTime = -1;
        }
    }

    private Thread mSeekThread;
    public void seek(){
        mSeekThread = getSeekThread();
        mSeekThread.start();
    }

    private Thread getSeekThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                pause();

                synchronized (this){
                    try{
                        this.wait(500);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

                if(mBroadcaster != null) {
                    long time =mFileReader.seek(100000);
                    mAudioTrackManager.seek(time);
                    mBroadcaster.seek(time);
                }

                resume();
            }
        });
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
        mAudioTrackManager.destroy();
        mAudioTrackManager = null;


        if(mFileReader != null){
            mFileReader.destroy();
            mFileReader = null;
        }

        if(mSntpClient != null){
            mSntpClient.destroy();
            mSntpClient = null;
        }

        mTimeManager = null;

        System.gc();
    }

}
