package io.unisong.android;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import io.unisong.android.activity.friends.contacts.ContactsLoader;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.MusicDataManager;
import io.unisong.android.audio.audio_track.AudioTrackManager;

/**
 * The MediaService is a long-running service used to provide Media data, such as the MusicDataManager.
 * Created by Ethan on 1/25/2015.
 */
public class MediaService extends Service{

    static final String LOG_TAG = MediaService.class.getSimpleName();

    private IBinder binder = new MediaServiceBinder();

    private ContactsLoader mContactsLoader;

    private BroadcastReceiver mMessageReceiver;
    private MusicDataManager musicDataManager;

    public static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";
    public static final String LARGE_TEST_FILE_PATH = "/storage/emulated/0/music/1hour.mp3";

    private AudioStatePublisher audioStatePublisher;

    private AudioTrackManager audioTrackManager;
    @Override
    public void onCreate(){
        //TODO : check server to see if this is the current application version ,and disable the app if its not
        //TODO : make sure we are IPv6 compatible
        super.onCreate();
        Log.d(LOG_TAG, "Starting MediaService");

        audioStatePublisher = AudioStatePublisher.getInstance();

        if(audioStatePublisher == null)
            audioStatePublisher = new AudioStatePublisher();
        // Create AudioStatePublisher first, because many other components will try to access it

        //The instanatiation of AudioTrackManager needs to be after that of TimeManager!
        // TODO : fix above bug
        audioTrackManager = new AudioTrackManager();


        musicDataManager = new MusicDataManager(getApplicationContext());
        MusicDataManager.setInstance(musicDataManager);

        // Start ConnectionUtils and assign it to the static instance.

    }

    public IBinder onBind(Intent arg0){
        return binder;
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
        Log.d(LOG_TAG , "Destroying audioTrackManager");

        if(musicDataManager != null)
            musicDataManager.destroy();

        musicDataManager = null;

        if(audioTrackManager != null)
            audioTrackManager.destroy();

        audioTrackManager = null;

        if(audioStatePublisher != null)
            audioStatePublisher.destroy();

        audioStatePublisher = null;

        if(musicDataManager != null)
            musicDataManager.destroy();

        musicDataManager = null;

    }


}
