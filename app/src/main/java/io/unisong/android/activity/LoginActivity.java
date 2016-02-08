package io.unisong.android.activity;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.unisong.android.PrefUtils;
import io.unisong.android.R;
import io.unisong.android.activity.register.FBPhoneNumberInputActivity;
import io.unisong.android.activity.register.RegisterActivity;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.FacebookAccessToken;
import io.unisong.android.network.user.User;

/**
 * Created by ezturner on 7/14/2015.
 */
public class LoginActivity extends ActionBarActivity {

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    private Toolbar toolbar;

    private HttpClient client;

    private String username, password;
    private FloatLabel usernameLabel;
    private FloatLabel passwordLabel;
    private boolean loginInProgress;

    @Nullable
    private LoginResult facebookLoginResult;
    private LoginButton loginButton;
    private CallbackManager callbackManager;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        client = HttpClient.getInstance();


        Log.d(LOG_TAG, "LoginActivity onCreate called");
        usernameLabel = (FloatLabel) findViewById(R.id.loginUsername);
        passwordLabel = (FloatLabel) findViewById(R.id.login_password);

        toolbar = (Toolbar) findViewById(R.id.login_bar);

        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) this.findViewById(R.id.facebook_login_button);
        List<String> readPermissions = new ArrayList<>();
        readPermissions.add("user_friends");
        readPermissions.add("email");
        loginButton.setReadPermissions(readPermissions);


        // Other app specific specialization

        // Callback registration
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                Log.d(LOG_TAG, loginResult.getAccessToken().toString());
                Log.d(LOG_TAG, "Login Success!");

                Log.d(LOG_TAG , "Facebook expiration date is : " + AccessToken.getCurrentAccessToken().getExpires());

                PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "facebook");
                FacebookAccessToken.saveFacebookAccessToken(getApplicationContext());
                facebookLoginResult = loginResult;
                loginFB();

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

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        loginInProgress = false;
        setCredentials();

    }

    /**
     * First checks that the selected Facebook User has an Unisong account
     * If so, then we will log them in. If not, then we will proceed to the Registration flow.
     */
    public void loginFB(){
        client.get(NetworkUtilities.HTTP_URL + "/user/get-by-facebookID/" + facebookLoginResult.getAccessToken().getUserId(), checkFacebookUser);
    }

    private Callback checkFacebookUser = new Callback() {
        @Override
        public void onFailure(Request request, IOException e) {
            Log.d(LOG_TAG , "CheckFacebookUser failed!");
            e.printStackTrace();

        }

        @Override
        public void onResponse(Response response) throws IOException {
            if(response.toString().contains("code=404")){
                // If no user exists with that FB ID then we will proceed to the PhoneNumberInputFBActivity

                GraphRequest.GraphJSONObjectCallback graphCallback = new GraphRequest.GraphJSONObjectCallback() {
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
                        AccessToken.getCurrentAccessToken(),graphCallback);

                Log.d(LOG_TAG, "Response contained 404, sending GraphRequest for email.");
                Bundle parameters = new Bundle();
                parameters.putString("fields", "email");
                request.setParameters(parameters);
                request.executeAsync();

            } else if(response.toString().contains("code=200")){
                //If a user exists with that FB ID then we can proceed straight to FriendsActivity

                client.loginFacebook(facebookLoginResult.getAccessToken());

                startActivity(UnisongActivity.class);

            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode,
                resultCode, data);
    }

    public void login(View view){
        PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "unisong");
        if(!loginInProgress){
            loginInProgress = true;
            loginRequest();
            //            loginThread = getLoginThread();
//            loginThread.start();
        }

    }

    private void loginRequest(){
        HttpClient client = HttpClient.getInstance();

        username = this.usernameLabel.getEditText().getText().toString();
        password = this.passwordLabel.getEditText().getText().toString();


        JSONObject object = new JSONObject();

        try {
            object.put("username", username);
            object.put("password", password);
        } catch (JSONException e){
            e.printStackTrace();
        }

        Log.d(LOG_TAG, "Sending Login Request");
        client.post(NetworkUtilities.HTTP_URL + "/login", object, userPassLoginCallback);

        //TODO: save credentials with AccountManager
    }

    private Callback userPassLoginCallback = new Callback() {
        @Override
        public void onFailure(Request request, IOException e) {

        }

        @Override
        public void onResponse(Response response) throws IOException {
            if(response.code() == 200) {
                Log.d(LOG_TAG, "Login Success");
                PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "unisong");
                CurrentUser user = new CurrentUser(getApplicationContext(), new User(username, password));
                client.loginSuccess();
                loginSuccess();
            } else {
                Log.d(LOG_TAG , "Login Failure");
                client.loginFailure();
                loginFailure();
            }

            Log.d(LOG_TAG, "# cookies: " + client.getCookieManager().getCookieStore().getCookies().size());


            loginInProgress = false;
        }
    };

    private void loginSuccess(){
        AccountManager manager = AccountManager.get(this);
        PrefUtils.saveToPrefs(this, PrefUtils.PREFS_LOGIN_USERNAME_KEY, username);
        PrefUtils.saveToPrefs(this , PrefUtils.PREFS_LOGIN_PASSWORD_KEY , password);
        startActivity(UnisongActivity.class);
    }

    private void startActivity(Class classToStart){
        Intent intent = new Intent(this, classToStart);
        startActivity(intent);
        finish();
    }

    private void loginFailure(){
        runOnUiThread(() -> {
            // Todo : tell the user if they got the password or username wrong.
            Toast toast = Toast.makeText(getApplicationContext(), "Login Failed!", Toast.LENGTH_LONG);
            toast.show();
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
            this.usernameLabel.setText(username);
        }

        if(!password.equals("")){
            this.passwordLabel.setText(password);
        }
    }

}
