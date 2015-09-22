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
import java.net.HttpCookie;
import java.util.List;

import io.unisong.android.network.user.FacebookAccessToken;
import io.unisong.android.PrefUtils;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;

/**
 * Created by Ethan on 7/28/2015.
 * This is the class where a sizeable portion of the HTTP communication is handled. Other classes
 * can use the interface to do their own requests.
 * This handles session state and login information.
 * It uses the Singleton design pattern.
 */
public class HttpClient {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String LOG_TAG = HttpClient.class.getSimpleName();

    private AccessToken mFBAccessToken;
    private boolean mIsLoggedIn;
    private static HttpClient sInstance;

    private CookieManager mManager;

    private OkHttpClient mClient;
    private boolean mLoginDone;
    private Context mContext;

    public HttpClient(Context context){
        mClient = new OkHttpClient();
        // TODO : reenable session persistence.
        mManager = new CookieManager(new PersistentCookieStore(context) , CookiePolicy.ACCEPT_ALL);
        //mManager = new CookieManager();
        //mManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        mClient.setCookieHandler(mManager);
        mIsLoggedIn = false;
        mLoginDone = false;
        sInstance = this;
        mContext = context;
    }

    public void login(String username, String password){
        mLoginDone = false;
        getLoginThread(username , password).start();
    }

    public OkHttpClient getClient(){
        return mClient;
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
                    response = post(NetworkUtilities.HTTP_URL + "/login", object);
                } catch (IOException e){
                    e.printStackTrace();
                    Log.d(LOG_TAG, "Request Failed");
                    mIsLoggedIn = false;
                    mLoginDone = true;
                    return;
                }

                if(response.code() == 200) {
                    Log.d(LOG_TAG , "Login Success");
                    PrefUtils.saveToPrefs(mContext, PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "unisong");
                    mIsLoggedIn = true;
                } else {
                    Log.d(LOG_TAG , "Login Failure");
                    mIsLoggedIn = false;
                }

                mLoginDone = true;

            }
        });
    }

    public Response post(String url, JSONObject json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = mClient.newCall(request).execute();
        return response;
    }

    public Response get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
        .build();
        Response response = mClient.newCall(request).execute();
        return response;
    }

    public Response put(String url, JSONObject json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url( url)
                .put(body)
                .build();
        Response response = mClient.newCall(request).execute();
        return response;
    }

    public Response delete(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
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

    public void checkIfLoggedIn(){



        String accountType = PrefUtils.getFromPrefs(mContext, PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "");

        if(accountType.equals("facebook")){
            FacebookAccessToken.loadFacebookAccessToken(mContext);

            AccessToken token = AccessToken.getCurrentAccessToken();
            if(token != null){
                mFBAccessToken = token;
            } else {
                mLoginDone = true;
                return;
            }
        }


        List<HttpCookie> cookies = mManager.getCookieStore().getCookies();

        for(HttpCookie cookie : cookies){
            if(cookie.getName().equals("connect.sid") && !cookie.hasExpired()){
                Log.d(LOG_TAG , "Cookie found, we are logged in.");
                mIsLoggedIn = true;
                mLoginDone = true;

                CurrentUser user = new CurrentUser(mContext, accountType);

                return;
            }
        }

        if(accountType.equals("facebook")){
            loginFacebook(mFBAccessToken);
        }

        String username = PrefUtils.getFromPrefs(mContext , PrefUtils.PREFS_LOGIN_USERNAME_KEY , "");
        String password = PrefUtils.getFromPrefs(mContext , PrefUtils.PREFS_LOGIN_PASSWORD_KEY , "");

        if(!username.equals("") && !password.equals("")){
            login(username , password);
            return;
        }

        mLoginDone = true;


    }

    // Fields used for facebook login/register
    // TODO : refractor and reorganize

    private String mEmail;
    private String mUsername;
    private String mPhoneNumber;

    public void loginFacebook(AccessToken token, String email, String username, String phonnenumber){
        //AccessToken tokenld = new AccessToken();
        mEmail = email;
        mPhoneNumber = phonnenumber;
        mUsername = username;
        loginFacebook(token);
    }

    public void loginFacebook(AccessToken token){
        //AccessToken tokenld = new AccessToken();
        mFBAccessToken = token;
        getFBLoginThread().start();
    }

    private Thread getFBLoginThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject loginObject = new JSONObject();
                try {
                    loginObject.put("access_token", mFBAccessToken.getToken());
                    if(mEmail != null && mPhoneNumber != null && mUsername != null){
                        loginObject.put("email" , mEmail);
                        loginObject.put("phone_number" , mPhoneNumber);
                        loginObject.put("username" , mUsername);
                        // TODO : get rid of this and have a phone number verification step.
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                    mLoginDone = true;
                    return;
                }

                Response response;
                try {
                    response = post(NetworkUtilities.HTTP_URL + "/auth/facebook", loginObject);
                } catch (IOException e){
                    // TODO : handle
                    e.printStackTrace();
                    mLoginDone = true;
                    return;
                }


                if(response.code() == 200) {
                    Log.d(LOG_TAG , "Facebook Login Success");
                    PrefUtils.saveToPrefs(mContext, PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "facebook");
                    CurrentUser user = new CurrentUser(mContext , new User(mContext , mFBAccessToken));
                    mIsLoggedIn = true;
                } else {
                    Log.d(LOG_TAG , "Facebook Login Failure");
                    mIsLoggedIn = false;
                }

                mLoginDone = true;

            }
        });
    }
}
