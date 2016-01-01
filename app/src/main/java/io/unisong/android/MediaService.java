package io.unisong.android;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.unisong.android.activity.musicselect.MusicDataManager;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.AudioTrackManager;
import io.unisong.android.network.ConnectionUtils;
import io.unisong.android.network.DiscoveryHandler;
import io.unisong.android.network.TimeManager;
import io.unisong.android.network.client.Listener;
import io.unisong.android.network.host.Broadcaster;
import io.unisong.android.network.ntp.SntpClient;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.Contacts;
import io.unisong.android.network.user.User;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Ethan on 1/25/2015.
 */
public class MediaService extends Service{

    static final String LOG_TAG = MediaService.class.getSimpleName();

    private IBinder mBinder = new MediaServiceBinder();

    private Contacts mContacts;

    private BroadcastReceiver mMessageReceiver;
    private MusicDataManager mMusicDataManager;

    public static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";
    public static final String LARGE_TEST_FILE_PATH = "/storage/emulated/0/music/1hour.mp3";

    private AudioStatePublisher mAudioStatePublisher;

    private AudioTrackManager mAudioTrackManager;
    @Override
    public void onCreate(){
        //TODO : check server to see if this is the current application version ,and disable the app if its not
        //TODO : make sure we are IPv6 compatible
        super.onCreate();
        Log.d(LOG_TAG, "Starting MediaService");

        mAudioStatePublisher = AudioStatePublisher.getInstance();

        //The instanatiation of AudioTrackManager needs to be after that of TimeManager!
        // TODO : fix above bug
        mAudioTrackManager = new AudioTrackManager();

        mMusicDataManager = MusicDataManager.getInstance();

        if(mMusicDataManager == null){
            mMusicDataManager = new MusicDataManager(getApplicationContext());
            MusicDataManager.setInstance(mMusicDataManager);
        }
        // Start ConnectionUtils and assign it to the static instance.

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
        Log.d(LOG_TAG , "Destroying mAudioTrackManager");

        mMusicDataManager.destroy();
        mMusicDataManager = null;

        System.gc();
    }

}
