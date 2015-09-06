package io.unisong.android.network;


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

    public HttpClient(){
        mClient = new OkHttpClient();
        mManager = new CookieManager();
        mManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        mClient.setCookieHandler(mManager);
        mIsLoggedIn = false;
    }

    public void login(String username, String password){
        getLoginThread(username , password).start();
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
                Response response = null;
                try {
                    response = post(URL, json);
                } catch (IOException e){
                    e.printStackTrace();
                    Log.d(LOG_TAG, "Request Failed");
                    return;
                }

                if(response.toString().contains("code=200")) {
                    mIsLoggedIn = true;
                } else {
                    mIsLoggedIn = false;
                }

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

    public CookieStore getCookies(){
        return mManager.getCookieStore();
    }

    public static HttpClient getInstance(){
        if(sInstance == null){
            sInstance = new HttpClient();
        }
        return sInstance;
    }
}
