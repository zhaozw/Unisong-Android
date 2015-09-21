package io.unisong.android;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.facebook.AccessToken;

import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.AudioTrackManager;
import io.unisong.android.network.TimeManager;
import io.unisong.android.network.client.Listener;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.master.Broadcaster;
import io.unisong.android.network.ntp.SntpClient;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.Contacts;

/**
 * Created by Ethan on 1/25/2015.
 */
public class MediaService extends Service{

    static final String LOG_TAG = MediaService.class.getSimpleName();

    private IBinder mBinder = new MediaServiceBinder();

    private Contacts mContacts;
    private Listener mListener;
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
        //mListener = new Listener(this);
    }

    @Override
    public void onCreate(){
        //TODO : check server to see if this is the current application version ,and disable the app if its not
        //TODO : make sure we are IPv6 compatible
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

        mContacts = new Contacts(getApplicationContext());
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

//        AudioFileReader reader = new AudioFileReader();
//        try {
//            reader.readFile(TEST_FILE_PATH);
//        } catch (IOException e){
//            e.printStackTrace();
//        }

        if(UnisongSession.getInstance() == null){
            UnisongSession session = new UnisongSession();
        }
    }

    public void broadcaster() {
        if(mBroadcaster == null) {
            mBroadcaster = new Broadcaster();
        }
    }


    public void listener(){
        mListener = new Listener();
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
