package com.ezturner.audiotracktest;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.ezturner.audiotracktest.MediaService.MediaServiceBinder;
import com.ezturner.audiotracktest.network.Master;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity {

    //The MediaService that does basically everything
    private MediaService mMediaService;

    //A boolean so that we don't get an error from doing a bunch of startup stuff multiple times
    //Since onCreate is called every time the screen orientation changes
    private boolean hasStarted = false;

    private boolean mMasterReceived;

    //The phone's phone number
    private static String sPhoneNumber;

    //Objects for enabling multicast
    private static WifiManager wifiManager;
    private static WifiManager.MulticastLock mCastLock;

    //The list of master devices that can be connected to
    private ArrayList<Master> mMasters;


    //The handler for interacting with the UI thread
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(!hasStarted) {

            //Create the activity and set the layout
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            //Start MediaService
            Intent ServiceIntent = new Intent(this, MediaService.class);
            bindService(ServiceIntent, mediaConnection, Context.BIND_AUTO_CREATE);

            //Start the Multicast manager objects
            wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            mCastLock = wifiManager.createMulticastLock("mydebuginfo");


            //Register the broadcast reciever
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                    new IntentFilter("master-discovered"));

            //Get the phone number
            TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
            sPhoneNumber = tMgr.getLine1Number();

            //TODO: Set a placeholder number for when the above method doesn't work
            if(sPhoneNumber == null){
                sPhoneNumber = "";
            }

            mMasterReceived = false;

            mHandler = new Handler(Looper.getMainLooper()) {

                /*
                * handleMessage() defines the operations to perform when
                * the Handler receives a new Message to process.
                */
                @Override
                public void handleMessage(Message inputMessage) {
                    // Gets the image task from the incoming Message object.
                    if(!mMediaService.isPlaying()) {
                        Master master = (Master) inputMessage.obj;
                        mMasters.add(master);
                    }

                    //If this is the first master recieved, start the timer and set mMasterReceived to true
                    if(!mMasterReceived && !mMediaService.isPlaying()){
                        mHandler.postDelayed(mPromptUserForStreams , 35);
                        mMasterReceived = true;
                    }
                }


            };
        }
        hasStarted = true;

    }

    //The runnable that will prompt users for which stream they'd like to join if there are several.
    private Runnable mPromptUserForStreams = new Runnable() {
        @Override
        public void run() {
            mMasterReceived = false;

            //TODO: Prompt user to choose which stream to play

            mMediaService.playFromMaster(mMasters.get(0));
        }
    };

    //connect to the service
    private ServiceConnection mediaConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            MediaServiceBinder binder = (MediaServiceBinder)service;
            //get service
            mMediaService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void togglePlay(View v){
        mMediaService.togglePlay();
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

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
        }
    };

    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

}
