package io.unisong.android.network.http;


import android.content.Context;
import android.util.Log;

import com.facebook.AccessToken;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;

import io.unisong.android.PrefUtils;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.user.CurrentUser;

/**
 * Created by Ethan on 7/28/2015.
 * This is the class where all of the Http communication is handled
 * It uses the Singleton design pattern.
 */
public class HttpClient {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String LOG_TAG = HttpClient.class.getSimpleName();

    private AccessToken mFBAccessToken;
    private String mEmail;
    private boolean mIsLoggedIn;
    private static HttpClient sInstance;
    private boolean mIsDoneCheckingLoginStatus;

    private CookieManager mManager;

    private OkHttpClient mClient;
    private boolean mLoginDone;
    private Context mContext;

    public HttpClient(Context context){
        mIsDoneCheckingLoginStatus = false;
        mClient = new OkHttpClient();
        mManager = new CookieManager(new PersistentCookieStore(context) , CookiePolicy.ACCEPT_ALL);
        //mManager = new CookieManager();
        //mManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        mClient.setCookieHandler(mManager);
        mIsLoggedIn = false;
        sInstance = this;
        mContext = context;

        checkIfLoggedIn();
    }

    public void login(String username, String password){
        mLoginDone = false;
        getLoginThread(username , password).start();
    }

    public boolean isLoginDone(){
        return mLoginDone;
    }

    public boolean isLoggedIn(){
        return mIsLoggedIn;
    }

    private Thread getLoginThread(final String username, final String password){
        return new Thread(new Runnable() {
            @Override
            public void run() {

                JSONObject object = new JSONObject();

                try {
                    object.put("username", username);
                    object.put("password", password);
                } catch (JSONException e){
                    e.printStackTrace();
                }

                Log.d(LOG_TAG, "Sending Login Request");
                Response response;
                try {
                    response = post("/login", object);
                } catch (IOException e){
                    e.printStackTrace();
                    Log.d(LOG_TAG, "Request Failed");
                    mIsLoggedIn = false;
                    mLoginDone = true;
                    return;
                }

                if(response.toString().contains("code=200")) {
                    Log.d(LOG_TAG , "Login Success");
                    mIsLoggedIn = true;
                } else {
                    Log.d(LOG_TAG , "Login Failure");
                    mIsLoggedIn = false;
                }

                mLoginDone = true;

                // If we're auto-logging in then say we're done checking login status.
                if(!mIsDoneCheckingLoginStatus){
                    mIsDoneCheckingLoginStatus = true;
                }

            }
        });
    }

    public Response post(String url, JSONObject json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(NetworkUtilities.HTTP_URL + url)
                .post(body)
                .build();
        Response response = mClient.newCall(request).execute();
        return response;
    }

    public Response get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(NetworkUtilities.HTTP_URL + url)
                .get()
                .build();
        Response response = mClient.newCall(request).execute();
        return response;
    }

    public Response put(String url, JSONObject json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(NetworkUtilities.HTTP_URL + url)
                .put(body)
                .build();
        Response response = mClient.newCall(request).execute();
        return response;
    }

    public Response delete(String url) throws IOException {
        Request request = new Request.Builder()
                .url(NetworkUtilities.HTTP_URL + url)
                .delete()
                .build();
        Response response = mClient.newCall(request).execute();
        return response;
    }

    public CookieManager getCookieManager(){
        return mManager;
    }

    public static HttpClient getInstance(){
        return sInstance;
    }

    public boolean isDoneCheckingLoginStatus(){
        return mIsDoneCheckingLoginStatus;
    }
    private void checkIfLoggedIn(){

        List<HttpCookie> cookies = mManager.getCookieStore().getCookies();

        for(HttpCookie cookie : cookies){
            if(cookie.getName().equals("connect.sid") && !cookie.hasExpired()){
                Log.d(LOG_TAG , "Cookie found, we are logged in.");
                mIsLoggedIn = true;
                CurrentUser user = new CurrentUser(mContext, "unisong");
                mIsDoneCheckingLoginStatus = true;
                return;
            }
        }

        String username = PrefUtils.getFromPrefs(mContext , PrefUtils.PREFS_LOGIN_USERNAME_KEY , "");
        String password = PrefUtils.getFromPrefs(mContext , PrefUtils.PREFS_LOGIN_PASSWORD_KEY , "");
        if(!username.equals("") && !password.equals("")){
            login(username , password);
        }
    }

    public void loginFacebook(AccessToken token, String email){
        mFBAccessToken = token;
        mEmail = email;
        getFBLoginThread().start();
    }

    private Thread getFBLoginThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject loginObject = new JSONObject();
                try {
                    loginObject.put("access_token", mFBAccessToken.getToken());
                    if(mEmail != null){
                        loginObject.put("email" , mEmail);
                        // TODO : get rid of this and have a phone number verification step.
                        loginObject.put("username" , "fbuser" + new Random().nextInt(500000));
                        loginObject.put("phone_number" , "2399244923");
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                    return;
                }

                Response response;
                try {
                    response = post("/auth/facebook", loginObject);
                } catch (IOException e){
                    // TODO : handle
                    e.printStackTrace();
                    return;
                }


                if(response.toString().contains("code=200")) {
                    Log.d(LOG_TAG , "Facebook Login Success");
                    mIsLoggedIn = true;
                } else {
                    Log.d(LOG_TAG , "Facebook Login Failure");
                    mIsLoggedIn = false;
                }

                // TODO : get long term access token from here.

            }
        });
    }
}
