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
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.iangclifton.android.floatlabel.FloatLabel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.unisong.android.R;

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

    private LoginButton mLoginButton;
    private CallbackManager mCallbackManager;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);
        if(HttpClient.getInstance() == null) {
            mClient = new HttpClient(this);
        } else {
            mClient = HttpClient.getInstance();
        }




        Log.d(LOG_TAG, "LoginActivity onCreate called");
        mUsername = (FloatLabel) findViewById(R.id.loginUsername);
        mPassword = (FloatLabel) findViewById(R.id.loginPassword);

        mToolbar = (Toolbar) findViewById(R.id.login_bar);

        mCallbackManager = CallbackManager.Factory.create();
        mLoginButton = (LoginButton) this.findViewById(R.id.facebook_login_button);
        List<String> readPermissions = new ArrayList<>();
        readPermissions.add("user_friends");
        readPermissions.add("email");
        mLoginButton.setReadPermissions(readPermissions);


        // Other app specific specialization

        // Callback registration
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                Log.d(LOG_TAG, loginResult.getAccessToken().toString());
                Log.d(LOG_TAG, "Login Success!");


                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(
                                    JSONObject object,
                                    GraphResponse response) {
                                // Application code
                                Log.v("LoginActivity", response.toString());

                                try {
                                     mClient.loginFacebook(loginResult.getAccessToken(), response.getJSONObject().getString("email"));
                                } catch (JSONException e){
                                    e.printStackTrace();
                                    return;
                                }

                                Intent intent = new Intent(getApplicationContext(), FriendsListActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "email");
                request.setParameters(parameters);
                request.executeAsync();

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

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mLoginInProgress = false;
        setCredentials();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode,
                resultCode, data);
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
