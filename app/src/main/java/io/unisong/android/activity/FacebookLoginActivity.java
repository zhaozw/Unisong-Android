package io.unisong.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import io.unisong.android.R;
import io.unisong.android.activity.Friends.FriendsListActivity;

/**
 * Created by ezturner on 9/14/2015.
 */
public class FacebookLoginActivity extends ActionBarActivity {

    private final static String LOG_TAG = FacebookLoginActivity.class.getSimpleName();

    private LoginButton mLoginButton;
    private CallbackManager mCallbackManager;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_facebook_login);

        Log.d(LOG_TAG, "FacebookLoginActivity");

        mCallbackManager = CallbackManager.Factory.create();
        mLoginButton = (LoginButton) this.findViewById(R.id.facebook_login_button);
        mLoginButton.setReadPermissions("user_friends");
        mLoginButton.setReadPermissions("");
        // Other app specific specialization

        // Callback registration
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(LOG_TAG , loginResult.getAccessToken().toString());
                Log.d(LOG_TAG, "Login Success!");

                Intent intent = new Intent(getApplicationContext(), FriendsListActivity.class);
                startActivity(intent);
                finish();
                // App code
            }

            @Override
            public void onCancel() {
                Log.d(LOG_TAG, "Login Cancelled");
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                exception.printStackTrace();
                Log.d(LOG_TAG, "Facebook exception thrown.");
                // App code
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode,
                resultCode, data);
    }

}
