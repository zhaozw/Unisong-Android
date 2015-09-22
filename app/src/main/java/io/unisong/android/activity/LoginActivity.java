package io.unisong.android.activity;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import io.unisong.android.network.user.FacebookAccessToken;
import io.unisong.android.PrefUtils;
import io.unisong.android.activity.Friends.FriendsListActivity;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.iangclifton.android.floatlabel.FloatLabel;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
        setContentView(R.layout.activity_login);

        mClient = HttpClient.getInstance();


        Log.d(LOG_TAG, "LoginActivity onCreate called");
        mUsername = (FloatLabel) findViewById(R.id.loginUsername);
        mPassword = (FloatLabel) findViewById(R.id.login_password);

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

                Log.d(LOG_TAG , "Facebook expiration date is : " + AccessToken.getCurrentAccessToken().getExpires());

                PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "facebook");
                FacebookAccessToken.saveFacebookAccessToken(getApplicationContext());
                loginFB(loginResult);

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

    public void loginFB(LoginResult loginResult){
        getLoginFBThread(loginResult).start();

    }

    private Thread getLoginFBThread(final LoginResult loginResult){
        return new Thread(new Runnable() {
            @Override
            public void run() {

                Response httpResponse;

                try {
                    httpResponse = mClient.get(NetworkUtilities.HTTP_URL + "/user/get-by-facebookID/" + loginResult.getAccessToken().getUserId());
                } catch (IOException e){
                    e.printStackTrace();
                    return;
                }

                // TODO : try to see if the response says "User not found." instead of just a 404 code.
                if(httpResponse.toString().contains("code=404")){
                    // If no user exists with that FB ID then we will proceed to the PhoneNumberInputFBActivity

                    GraphRequest.GraphJSONObjectCallback callback = new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(
                                JSONObject object,
                                GraphResponse response) {
                            // Application code

                            Log.d(LOG_TAG , "GraphRequest is done, email has been received.");

                            Intent intent = new Intent(getApplicationContext(), FBPhoneNumberInputActivity.class );

                            try {
                                intent.putExtra("email", response.getJSONObject().getString("email").toCharArray());
                                startActivity(intent);

                            } catch (JSONException e){
                                e.printStackTrace();
                            }


                        }


                    };

                    GraphRequest request = GraphRequest.newMeRequest(
                            AccessToken.getCurrentAccessToken(),callback);

                    Log.d(LOG_TAG, "Response contained 404, sending GraphRequest for email.");
                    Bundle parameters = new Bundle();
                    parameters.putString("fields", "email");
                    request.setParameters(parameters);
                    //request.executeAsync();

                    // TODO : investigate why executeAsync wasn't working.
                    request.executeAndWait();



                } else if(httpResponse.toString().contains("code=200")){
                    //If a user exists with that FB ID then we can proceed straight to FriendsActivity

                    mClient.loginFacebook(loginResult.getAccessToken());

                    startActivity(UnisongActivity.class);

                }
            }
        });
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode,
                resultCode, data);
    }

    public void login(View view){
        PrefUtils.saveToPrefs(getApplicationContext() , PrefUtils.PREFS_ACCOUNT_TYPE_KEY , "unisong");
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
        startActivity(FriendsListActivity.class);
    }

    private void startActivity(Class classToStart){
        Intent intent = new Intent(this, classToStart);
        startActivity(intent);
        finish();
    }

    private void loginFailure(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Todo : tell the user if they got the password or username wrong.
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
