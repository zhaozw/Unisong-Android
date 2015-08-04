package com.ezturner.speakersync.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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

import com.ezturner.speakersync.MediaService;
import com.ezturner.speakersync.MyApplication;
import com.ezturner.speakersync.R;
import com.ezturner.speakersync.network.Master;
import com.ezturner.speakersync.MediaService.MediaServiceBinder;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity {

    //The MediaService that does basically everything
    private MediaService mMediaService;


    private boolean mMasterReceived;

    //The list of master devices that can be connected to
    private ArrayList<Master> mMasters;


    //The handler for interacting with the UI thread
    private Handler mHandler;

    //The Intent
    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //A boolean so that we don't get an error from doing a bunch of startup stuff multiple times
        //Since onCreate is called every time the screen orientation changes

        boolean hasStarted = false;


        //Get the phone number
        TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        MyApplication.setPhoneNumber(tMgr.getLine1Number());

        //TODO: Set a placeholder number for when the above method doesn't work
        if(MyApplication.getPhoneNumber() == null){
            MyApplication.setPhoneNumber("");
        }
        //Create the activity and set the layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIntent = getIntent();
        hasStarted = mIntent.getBooleanExtra("has-started" , false);

        if(!hasStarted) {

            //Start MediaService
            Intent ServiceIntent = new Intent(this, MediaService.class);
            startService(ServiceIntent);


            mIntent.putExtra("has-started"  , true);
        }

        //Register the broadcast reciever
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("master-discovered"));

        mMasterReceived = false;

        mHandler = new Handler(Looper.getMainLooper()) {

            /*
            * handleMessage() defines the operations to perform when
            * the Handler receives a new Message to process.
            */
            @Override
            public void handleMessage(Message inputMessage) {
                // Gets the image task from the incoming Message object.

            }


        };

    }

    //The runnable that will prompt users for which stream they'd like to join if there are several.
    private Runnable mPromptUserForStreams = new Runnable() {
        @Override
        public void run() {
            mMasterReceived = false;

            //TODO: Prompt user to choose which stream to play


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
        Intent intent = new Intent("service-interface");
        // You can also include some extra data.
        intent.putExtra("command" , "play");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void listener(View v){
        Intent intent = new Intent("service-interface");
        // You can also include some extra data.
        intent.putExtra("command" , "listener");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void broadcaster(View v){
        Intent intent = new Intent("service-interface");
        // You can also include some extra data.
        intent.putExtra("command" , "broadcaster");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        //Call google analytics, and tell them they did this action
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);

            /*if(!MyApplication.isPlaying()) {


                Master master = (Master) intent.getSerializableExtra("master");
                mMasters.add(master);
            }*/

        }
    };

    @Override
    protected void onDestroy() {
        Intent intent = new Intent("service-interface");
        // You can also include some extra data.
        intent.putExtra("command" , "destroy");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    public void pause(View v){
        Intent intent = new Intent("service-interface");
        // You can also include some extra data.
        intent.putExtra("command", "pause");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void resume(View v){
        Intent intent = new Intent("service-interface");
        // You can also include some extra data.
        intent.putExtra("command" , "resume");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void seek(View v){
        Intent intent = new Intent("service-interface");
        // You can also include some extra data.
        intent.putExtra("command" , "seek");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void retransmit(View v){
        Intent intent = new Intent("service-interface");
        // You can also include some extra data.
        intent.putExtra("command" , "retransmit");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
