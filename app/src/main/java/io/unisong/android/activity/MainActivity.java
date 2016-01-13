package io.unisong.android.activity;

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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import io.unisong.android.MediaService;
import io.unisong.android.MyApplication;
import io.unisong.android.PrefUtils;
import io.unisong.android.R;
import io.unisong.android.network.Host;
import io.unisong.android.MediaService.MediaServiceBinder;
import io.unisong.android.network.NetworkService;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.user.FriendsList;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = MainActivity.class.getSimpleName();

    //The handler for interacting with the UI thread
    private static Handler sHandler;

    private NetworkService mNetworkService;
    private MediaService mMediaService;
    private HttpClient mClient;

    //The Intent
    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //A boolean so that we don't get an error from doing a bunch of startup stuff multiple times
        //Since onCreate is called every time the screen orientation changes

        boolean hasStarted = false;

        //Create the activity and set the layout
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        mIntent = getIntent();
        hasStarted = mIntent.getBooleanExtra("has-started", false);


        if(!hasStarted) {

            //Start MediaService
            Intent ServiceIntent = new Intent(getApplicationContext(), NetworkService.class);
            bindService(ServiceIntent, mNetworkConnection, Context.BIND_AUTO_CREATE);
            ServiceIntent = new Intent(getApplicationContext(), MediaService.class);
            bindService(ServiceIntent, mMediaConnection, Context.BIND_AUTO_CREATE);


            mIntent.putExtra("has-started"  , true);
        }

        if(sHandler == null) {
            sHandler = new Handler(Looper.getMainLooper()) {

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

        if(PrefUtils.getFromPrefs(this , PrefUtils.PREFS_HAS_OPENED_APP_KEY , "no").equals("yes")) {
            getLoginCheckThread().start();
            return;
        }

        // TODO : analytics?
        PrefUtils.saveToPrefs(this, PrefUtils.PREFS_HAS_OPENED_APP_KEY, "yes");
        startNewActivity(LoginActivity.class);
    }

    private Thread getLoginCheckThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                checkIfLoggedIn();
            }
        });
    }

    private void checkIfLoggedIn() {

        while(mClient == null){
            synchronized (this){
                try{
                    this.wait(2);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            mClient = HttpClient.getInstance();
        }


        while(!mClient.isLoginDone()){
            synchronized (this){
                try{
                    this.wait(20);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }

        Log.d(LOG_TAG , "Login status retrieved.");

        // Check our current cookie-based login status
        if (mClient.isLoggedIn()) {
            // We are logged in, proceed to FriendsListActivity for now, and the new default for later.
            Log.d(LOG_TAG , "We are logged in! Starting UnisongActivity");
            startNewActivity(UnisongActivity.class);
        } else if(AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired()){
            // If we're logged in with facebook and don't have a cookie, but our access token
            // is not yet expired, then try to log in with the access token.
            // TODO : investigate facebook token expirations. They might autorenew.
            Log.d(LOG_TAG, "Our HTTP cookie has expired, but our facebook access token is still good.");

            mClient.loginFacebook(AccessToken.getCurrentAccessToken() );

        } else {
            Log.d(LOG_TAG, "We're not logged in and never were, starting LoginActivity");
            startNewActivity(LoginActivity.class);
        }

    }

    private void startNewActivity(Class classToStart){
        Intent intent = new Intent(getApplicationContext(), classToStart);
        startActivity(intent);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG , "MainActivity onDestroy() called, closing application.");

        if(mNetworkService != null)
            mNetworkService.onDestroy();

        if(mMediaService != null)
            mMediaService.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed();

        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    public void pause(View v){
        Intent intent = new Intent("unisong-service-interface");
        // You can also include some extra data.
        intent.putExtra("command", "pause");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void resume(View v){
        Intent intent = new Intent("unisong-service-interface");
        // You can also include some extra data.
        intent.putExtra("command" , "resume");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void seek(View v){
        Intent intent = new Intent("unisong-service-interface");
        // You can also include some extra data.
        intent.putExtra("command" , "seek");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void retransmit(View v){
        Intent intent = new Intent("unisong-service-interface");
        // You can also include some extra data.
        intent.putExtra("command" , "retransmit");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private ServiceConnection mNetworkConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            NetworkService.NetworkServiceBinder binder = (NetworkService.NetworkServiceBinder)service;
            //get service
            mNetworkService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private ServiceConnection mMediaConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            MediaService.MediaServiceBinder binder = (MediaService.MediaServiceBinder)service;
            //get service
            mMediaService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
}
