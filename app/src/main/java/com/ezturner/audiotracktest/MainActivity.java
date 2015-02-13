package com.ezturner.audiotracktest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.ezturner.audiotracktest.MediaService.MediaServiceBinder;

public class MainActivity extends ActionBarActivity {

    private MediaService mediaService;
    private boolean hasStarted = false;
    public static WifiManager wifiManager;
    public static WifiManager.MulticastLock mCastLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(!hasStarted) {
            Log.d("plz" , "work");
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            Intent ServiceIntent = new Intent(this, MediaService.class);
            bindService(ServiceIntent, mediaConnection, Context.BIND_AUTO_CREATE);


            wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);

            mCastLock = wifiManager.createMulticastLock("mydebuginfo");
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
            Log.d("ezturner", "test");
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




}
