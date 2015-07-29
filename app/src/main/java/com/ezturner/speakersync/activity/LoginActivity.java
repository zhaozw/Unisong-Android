package com.ezturner.speakersync.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.ezturner.speakersync.R;
import com.ezturner.speakersync.network.HttpClient;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.iangclifton.android.floatlabel.FloatLabel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by ezturner on 7/14/2015.
 */
public class LoginActivity extends ActionBarActivity {

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    private Toolbar mToolbar;

    private FloatLabel mUsername;
    private FloatLabel mPassword;

    private Thread mLoginThread;
    private boolean mLoginInProgress;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUsername = (FloatLabel) findViewById(R.id.loginUsername);
        mPassword = (FloatLabel) findViewById(R.id.loginPassword);

        mToolbar = (Toolbar) findViewById(R.id.login_bar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mLoginInProgress = false;
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

        String URL = NetworkUtilities.EC2_INSTANCE + "/login";
        JSONObject object = new JSONObject();

        try {
            object.put("username", username);
            object.put("password", password);
        } catch (JSONException e){
            e.printStackTrace();
        }
        String json = object.toString();

        String result = "Error!";
        try {
            result = client.post(URL, json);
        } catch (IOException e){
            e.printStackTrace();
        }

        Log.d(LOG_TAG , result);
        mLoginInProgress = false;
    }

    public void register(View view){
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

}
