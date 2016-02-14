package io.unisong.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.appevents.AppEventsLogger;

import io.unisong.android.MediaService;
import io.unisong.android.PrefUtils;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.network.NetworkService;
import io.unisong.android.network.http.HttpClient;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = MainActivity.class.getSimpleName();

    private Intent networkIntent;
    private Intent mediaIntent;
    private HttpClient client;

    //The Intent
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //A boolean so that we don't get an error from doing a bunch of startup stuff multiple times
        //Since onCreate is called every time the screen orientation changes

        boolean hasStarted = false;

        //Create the activity and set the layout
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        intent = getIntent();
        hasStarted = intent.getBooleanExtra("has-started", false);


        if(!hasStarted) {
            AudioStatePublisher publisher = new AudioStatePublisher();
            //Start MediaService
            networkIntent = new Intent(getApplicationContext(), NetworkService.class);
            startService(networkIntent);
            mediaIntent = new Intent(getApplicationContext(), MediaService.class);
            startService(mediaIntent);

            intent.putExtra("has-started", true);
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

        while(client == null){
            synchronized (this){
                try{
                    this.wait(2);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            client = HttpClient.getInstance();
        }


        while(!client.isLoginDone()){
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
        if (client.isLoggedIn()) {
            // We are logged in, proceed to FriendsListActivity for now, and the new default for later.
            Log.d(LOG_TAG , "We are logged in! Starting UnisongActivity");
            startNewActivity(UnisongActivity.class);
        } else if(AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired()){
            // If we're logged in with facebook and don't have a cookie, but our access token
            // is not yet expired, then try to log in with the access token.
            // TODO : investigate facebook token expirations. They might autorenew.
            Log.d(LOG_TAG, "Our HTTP cookie has expired, but our facebook access token is still good.");

            client.loginFacebook(AccessToken.getCurrentAccessToken());
            startNewActivity(UnisongActivity.class);

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

        if(networkIntent != null)
            stopService(networkIntent);
            else

        networkIntent = null;

        if(mediaIntent != null)
            stopService(mediaIntent);

        mediaIntent = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }
}
