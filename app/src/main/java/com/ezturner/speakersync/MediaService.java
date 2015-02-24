package com.ezturner.speakersync;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ezturner.speakersync.audio.AudioFileReader;
import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.audio.OpenMXPlayer;
import com.ezturner.speakersync.network.master.AudioBroadcaster;
import com.ezturner.speakersync.network.master.MasterDiscoveryHandler;
import com.ezturner.speakersync.network.slave.AudioListener;
import com.ezturner.speakersync.network.Master;

import java.io.IOException;

/**
 * Created by Ethan on 1/25/2015.
 */
public class MediaService extends Service{

    private IBinder mBinder = new MediaServiceBinder();

    private AudioListener mListener;
    private AudioBroadcaster mBroadcaster;
    private AudioTrackManager mAudioTrackManager;
    private AudioFileReader mFileReader;

    private MasterDiscoveryHandler mDiscovery;

    //Objects for enabling multicast
    private static WifiManager wifiManager;
    private static WifiManager.MulticastLock mCastLock;

    private static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";


    public MediaService(){
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
                }if(command.equals("play")){
                    Log.d("ezturner" , "Play received!");
                    play();
                }

            }
        };

        //Register the broadcast reciever
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("service-interface"));

        mAudioTrackManager = new AudioTrackManager();
        mFileReader = new AudioFileReader(mAudioTrackManager);
        try {
            mFileReader.extractorDecode();
        } catch(IOException e){
            e.printStackTrace();
        }

        //OpenMXPlayer player = new OpenMXPlayer();
        //player.setDataSource(TEST_FILE_PATH);
        //player.run();

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

    public void broadcaster() {
        if(mBroadcaster == null) {
            mBroadcaster = new AudioBroadcaster(mAudioTrackManager);
            mDiscovery = new MasterDiscoveryHandler(mBroadcaster);
        }
    }


    public void listener(){
        if(mAudioTrackManager == null) {
            mAudioTrackManager = new AudioTrackManager();

            mListener = new AudioListener(this, mAudioTrackManager);

            mListener.findMasters();
        }
    }

    public void play(){
        mAudioTrackManager.startPlaying();
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


    private BroadcastReceiver mMessageReceiver ;

}