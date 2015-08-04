package com.ezturner.speakersync.network;


import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * Created by Ethan on 7/28/2015.
 * This is the class where all of the Http communication is handled
 * It uses the Singleton design pattern.
 */
public class HttpClient {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static HttpClient sInstance;

    private CookieManager mManager;

    private OkHttpClient mClient;

    public HttpClient(){
        mClient = new OkHttpClient();
        mManager = new CookieManager();
        mManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        mClient.setCookieHandler(mManager);
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

    public String getCookies(){
        return mManager.getCookieStore().getCookies().get(0).toString();
    }

    public static HttpClient getInstance(){
        if(sInstance == null){
            sInstance = new HttpClient();
        }
        return sInstance;
    }
}
