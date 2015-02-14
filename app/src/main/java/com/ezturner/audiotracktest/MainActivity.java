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
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.ezturner.audiotracktest.MediaService.MediaServiceBinder;

public class MainActivity extends ActionBarActivity {

    private MediaService mediaService;
    private boolean hasStarted = false;

    private static String sPhoneNumber;

    private static WifiManager wifiManager;
    private static WifiManager.MulticastLock mCastLock;

    private IntentService mIntentService;

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

        }
        hasStarted = true;

    }

    //connect to the service
    private ServiceConnection mediaConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            MediaServiceBinder binder = (MediaServiceBinder)service;
            //get service
            mediaService = binder.getService();
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
        mediaService.togglePlay();
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
