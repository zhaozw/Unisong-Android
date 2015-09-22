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
import io.unisong.android.activity.Friends.FriendsListActivity;
import io.unisong.android.network.Master;
import io.unisong.android.MediaService.MediaServiceBinder;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.user.FriendsList;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity {

    private final static String LOG_TAG = MainActivity.class.getSimpleName();

    //The MediaService that does basically everything
    private MediaService mMediaService;


    private boolean mMasterReceived;

    //The list of master devices that can be connected to
    private ArrayList<Master> mMasters;


    //The handler for interacting with the UI thread
    private static Handler sHandler;

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
        FacebookSdk.sdkInitialize(getApplicationContext());
        //setContentView(R.layout.activity_main);

        mIntent = getIntent();
        hasStarted = mIntent.getBooleanExtra("has-started" , false);

        if(HttpClient.getInstance() == null){
            Log.d(LOG_TAG , "Starting HttpClient.");
            mClient = new HttpClient(getApplicationContext());
        }

        if(!hasStarted) {

            FriendsList friendsList = new FriendsList(getApplicationContext());
            //Start MediaService
            Intent ServiceIntent = new Intent(getApplicationContext(), MediaService.class);
            startService(ServiceIntent);


            mIntent.putExtra("has-started"  , true);
        }

        //Register the broadcast reciever
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("master-discovered"));

        mMasterReceived = false;

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
                checkIfLoggedIn();
            }
        });
    }

    private void checkIfLoggedIn() {

        Log.d(LOG_TAG , "Checking Login status");
        mClient.checkIfLoggedIn();

        Log.d(LOG_TAG , "Done with checkIfLoggedIn()");
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
            Log.d(LOG_TAG , "We are logged in! Starting FriendsListActivity");
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
        finish();
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
        intent.putExtra("command", "destroy");

        // TODO : find a better way to close the service when app is done.
        //LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
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
