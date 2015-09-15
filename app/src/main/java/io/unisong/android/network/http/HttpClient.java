package io.unisong.android.network.http;


import android.content.Context;
import android.util.Log;

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

    private boolean mIsLoggedIn;
    private static HttpClient sInstance;

    private CookieManager mManager;

    private OkHttpClient mClient;
    private boolean mLoginDone;
    private Context mContext;

    public HttpClient(Context context){
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
                String URL = NetworkUtilities.HTTP_URL + "/login";
                JSONObject object = new JSONObject();

                try {
                    object.put("username", username);
                    object.put("password", password);
                } catch (JSONException e){
                    e.printStackTrace();
                }
                String json = object.toString();

                Log.d(LOG_TAG, "Sending Login Request");
                Response response;
                try {
                    response = post(URL, json);
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

            }
        });
    }

    public Response post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
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

    public CookieManager getCookieManager(){
        return mManager;
    }

    public static HttpClient getInstance(){
        return sInstance;
    }

    private void checkIfLoggedIn(){

        List<HttpCookie> cookies = mManager.getCookieStore().getCookies();

        for(HttpCookie cookie : cookies){
            if(cookie.getName().equals("connect.sid") && !cookie.hasExpired()){
                mIsLoggedIn = true;
                CurrentUser user = new CurrentUser(mContext);
            }
        }
    }
}
