package io.unisong.android.network.http;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.facebook.AccessToken;
import com.squareup.okhttp.Callback;
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

import io.unisong.android.PrefUtils;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.FacebookAccessToken;
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

    private AccessToken fBAccessToken;
    private boolean isLoggedIn;
    private static HttpClient instance;

    private CookieManager manager;

    private OkHttpClient client;
    private boolean loginDone;
    private Context context;
    private Handler handler;

    public HttpClient(Context context){
        client = new OkHttpClient();
        // TODO : reenable session persistence.
        manager = new CookieManager(new PersistentCookieStore(context) , CookiePolicy.ACCEPT_ALL);
        //manager = new CookieManager();
        //manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client.setCookieHandler(manager);
        isLoggedIn = false;
        loginDone = false;
        instance = this;
        this.context = context;
        handler = new Handler();
        handler.postDelayed(mCheckCookieRunnable, 70 * 1000);
    }

    public void login(String username, String password){
        loginDone = false;
        getLoginThread(username , password).start();
    }

    public OkHttpClient getClient(){
        return client;
    }

    public boolean isLoginDone(){
        return loginDone;
    }

    public boolean isLoggedIn(){
        return isLoggedIn;
    }

    private Thread getLoginThread(final String username, final String password){
        return new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();
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
                    response = syncPost(NetworkUtilities.HTTP_URL + "/login", object);
                } catch (IOException e){
                    e.printStackTrace();
                    Log.d(LOG_TAG, "Request Failed");
                    isLoggedIn = false;
                    loginDone = true;
                    return;
                }

                if(response.code() == 200) {
                    Log.d(LOG_TAG, "Login Success");
                    PrefUtils.saveToPrefs(context, PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "unisong");
                    CurrentUser user = new CurrentUser(context, new User(username, password));
                    isLoggedIn = true;
                } else {
                    Log.d(LOG_TAG , "Login Failure");
                    isLoggedIn = false;
                }

                loginDone = true;

            }
        });
    }

    public void post(String url, JSONObject json, Callback callback) throws IOException {
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void get(String url , Callback callback) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
        .build();
        client.newCall(request).enqueue(callback);
    }

    public void put(String url, JSONObject json, Callback callback) throws IOException {
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url( url)
                .put(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void delete(String url, Callback callback) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        client.newCall(request).enqueue(callback);
    }

    public Response syncPost(String url, JSONObject json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response;
    }

    public Response syncGet(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Response response = client.newCall(request).execute();
        return response;
    }

    public Response syncPut(String url, JSONObject json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url( url)
                .put(body)
                .build();
        Response response = client.newCall(request).execute();
        return response;
    }

    public Response syncDelete(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        Response response = client.newCall(request).execute();
        return response;
    }

    public CookieManager getCookieManager(){
        return manager;
    }

    public static HttpClient getInstance(){
        return instance;
    }

    public void checkIfLoggedIn(){
        String accountType = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "");

        if(accountType.equals("facebook")){
            FacebookAccessToken.loadFacebookAccessToken(context);

            AccessToken token = AccessToken.getCurrentAccessToken();
            if(token != null){
                fBAccessToken = token;
            } else {
                loginDone = true;
                return;
            }
        }


        List<HttpCookie> cookies = manager.getCookieStore().getCookies();

        for(HttpCookie cookie : cookies){
            if(cookie.getName().equals("connect.sid") && !cookie.hasExpired()){
                Log.d(LOG_TAG , "Cookie found, we are logged in , account type is : " + accountType);
                isLoggedIn = true;
                loginDone = true;

                CurrentUser user = new CurrentUser(context, accountType);

                return;
            }
        }

        if(accountType.equals("facebook")){
            loginFacebook(fBAccessToken);
        }

        String username = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_LOGIN_USERNAME_KEY , "");
        String password = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_LOGIN_PASSWORD_KEY , "");

        if(!username.equals("") && !password.equals("")){
            login(username , password);
            return;
        }

        loginDone = true;

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
        fBAccessToken = token;
        getFBLoginThread().start();
    }

    private Thread getFBLoginThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                JSONObject loginObject = new JSONObject();
                try {
                    loginObject.put("access_token", fBAccessToken.getToken());
                    if(mEmail != null && mPhoneNumber != null && mUsername != null){
                        loginObject.put("email" , mEmail);
                        loginObject.put("phone_number" , mPhoneNumber);
                        loginObject.put("username" , mUsername);
                        // TODO : get rid of this and have a phone number verification step.
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                    loginDone = true;
                    return;
                }

                Response response;
                try {
                    response = syncPost(NetworkUtilities.HTTP_URL + "/auth/facebook", loginObject);
                } catch (IOException e){
                    // TODO : handle
                    e.printStackTrace();
                    loginDone = true;
                    return;
                }


                if(response.code() == 200) {
                    Log.d(LOG_TAG , "Facebook Login Success");
                    PrefUtils.saveToPrefs(context, PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "facebook");
                    CurrentUser user = new CurrentUser(context, new User(fBAccessToken));
                    isLoggedIn = true;
                } else {
                    Log.d(LOG_TAG , "Facebook Login Failure");
                    isLoggedIn = false;
                }

                loginDone = true;

            }
        });

    }

    private Runnable mCheckCookieRunnable = new Runnable() {
        @Override
        public void run() {

            List<HttpCookie> cookies = manager.getCookieStore().getCookies();

            for(HttpCookie cookie : cookies){
                if(cookie.getName().equals("connect.sid") ){
                    Log.d(LOG_TAG , "Cookie found, we are logged in.");

                    if(cookie.hasExpired()){
                        // TODO : renew cookie
                    } else if(cookie.getMaxAge() < (24 * 60 * 1000)){
                        handler.postDelayed(mReplaceCookieRunnable, 10 * 1000);
                    }

                    return;
                }
            }
        }
    };

    private Runnable mReplaceCookieRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                JSONObject credentials = new JSONObject();

                String username = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_LOGIN_USERNAME_KEY, "");
                String password = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_LOGIN_PASSWORD_KEY , "");

                credentials.put("username" , username);
                credentials.put("password" , password);
                syncPost(NetworkUtilities.HTTP_URL + "/login", credentials);
            } catch (IOException e){
                handler.postDelayed(mReplaceCookieRunnable, 100 * 1000);
                e.printStackTrace();
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
    };
}
