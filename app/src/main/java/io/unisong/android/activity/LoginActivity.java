package io.unisong.android.activity;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import io.unisong.android.PrefUtils;
import io.unisong.android.activity.Friends.FriendsListActivity;
import io.unisong.android.network.http.HttpClient;

import com.iangclifton.android.floatlabel.FloatLabel;

/**
 * Created by ezturner on 7/14/2015.
 */
public class LoginActivity extends ActionBarActivity {

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    private Toolbar mToolbar;

    private HttpClient mClient;

    private FloatLabel mUsername;
    private FloatLabel mPassword;

    private Thread mLoginThread;
    private boolean mLoginInProgress;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(HttpClient.getInstance() == null) {
            mClient = new HttpClient(this);
        } else {
            mClient = HttpClient.getInstance();
        }

        if(mClient.isLoggedIn()){
            startFriendActivity();
            return;
        }


        setContentView(io.unisong.android.R.layout.activity_login);

        mUsername = (FloatLabel) findViewById(io.unisong.android.R.id.loginUsername);
        mPassword = (FloatLabel) findViewById(io.unisong.android.R.id.loginPassword);

        mToolbar = (Toolbar) findViewById(io.unisong.android.R.id.login_bar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mLoginInProgress = false;
        setCredentials();

    }

    public void login(View view){
        if(!mLoginInProgress){
            mLoginInProgress = true;
            mLoginThread  = getLoginThread();
            mLoginThread.start();
        }

    }

    private Thread getLoginThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                loginRequest();
            }
        });
    }

    private void loginRequest(){
        HttpClient client = HttpClient.getInstance();

        String username = mUsername.getEditText().getText().toString();
        String password = mPassword.getEditText().getText().toString();

        mClient.login(username , password);
        while (!mClient.isLoginDone()) {
            try {
                synchronized (this) {
                    this.wait(20);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d(LOG_TAG , "Login Request Done.");

        //TODO: make code for various exceptions
        if(mClient.isLoggedIn()){
            loginSuccess(username , password);
        } else {
            loginFailure();
        }

        Log.d(LOG_TAG , "# cookies: " + mClient.getCookieManager().getCookieStore().getCookies().size());


        mLoginInProgress = false;
        //TODO: save credentials with AccountManager
    }

    private void loginSuccess(String username , String password){
        AccountManager manager = AccountManager.get(this);
        PrefUtils.saveToPrefs(this, PrefUtils.PREFS_LOGIN_USERNAME_KEY, username);
        PrefUtils.saveToPrefs(this , PrefUtils.PREFS_LOGIN_PASSWORD_KEY , password);
    }

    private void startFriendActivity(){
        Intent intent = new Intent(this, FriendsListActivity.class);
        startActivity(intent);
        finish();
    }

    private void loginFailure(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(), "Login Failed!", Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    public void register(View view){
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void setCredentials(){
        String username = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_LOGIN_USERNAME_KEY , "");
        String password = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_LOGIN_PASSWORD_KEY , "");

        if(!username.equals("")){
            mUsername.setText(username);
        }
        if(!password.equals("")){
            mPassword.setText(password);
        }
    }

}
