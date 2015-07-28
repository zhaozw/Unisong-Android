package com.ezturner.speakersync.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;

import com.ezturner.speakersync.R;
import com.ezturner.speakersync.network.HttpClient;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.iangclifton.android.floatlabel.FloatLabel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by ezturner on 7/15/2015.
 */
public class RegisterActivity extends ActionBarActivity {

    private static final String LOG_TAG = RegisterActivity.class.getSimpleName();

    private Toolbar mToolbar;

    private Thread mRegisterThread;
    private boolean mRegisterInProgress;

    private FloatLabel mUsername;
    private FloatLabel mPassword;
    private FloatLabel mPasswordVerify;
    private FloatLabel mPhoneNumber;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mUsername = (FloatLabel) findViewById(R.id.registerUsername);
        mPassword = (FloatLabel) findViewById(R.id.registerPassword);
        mPasswordVerify = (FloatLabel) findViewById(R.id.registerPasswordVerify);
        mPhoneNumber = (FloatLabel) findViewById(R.id.registerPhoneNumber);

        mToolbar = (Toolbar) findViewById(R.id.login_bar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    public void register(View view){

        String password = mPassword.getEditText().getText().toString();
        String passwordConfirm = mPasswordVerify.getEditText().getText().toString();

        if(!password.equals(passwordConfirm)){
            //TODO: tell the user that the password does not match
        }

        //TODO: uncomment and make a notifier for the registration process
//        if(!mRegisterInProgress){
            mRegisterInProgress = true;
            mRegisterThread = getRegisterThread();
            mRegisterThread.start();
//        }

    }

    private Thread getRegisterThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                registerRequest();
            }
        });
    }

    private void registerRequest() {

        Log.d(LOG_TAG , "Starting register request.");
        HttpClient client = HttpClient.getInstance();

        String username = mUsername.getEditText().getText().toString();
        String password = mPassword.getEditText().getText().toString();
        String phonenumber = mPhoneNumber.getEditText().getText().toString();

        if(!PhoneNumberUtils.isGlobalPhoneNumber(phonenumber)){
            //TODO: tell the user that this is not a valid phone number
        }
        String URL = NetworkUtilities.EC2_INSTANCE + "/register";
        JSONObject object = new JSONObject();

        try {
            object.put("username", username);
            object.put("password", password);
            object.put("phonenumber" , phonenumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String json = object.toString();
        String result;

        try {
            result = client.post(URL, json);
        } catch (IOException e) {
            e.printStackTrace();
            mRegisterInProgress = false;
            return;
        }

        Log.d(LOG_TAG , result);
        mRegisterInProgress = false;
    }
}
