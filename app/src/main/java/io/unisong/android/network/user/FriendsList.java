package io.unisong.android.network.user;

import android.util.Log;

import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;

/**
 * Created by Ethan on 8/9/2015.
 */
public class FriendsList {

    private final static String LOG_TAG = FriendsList.class.getSimpleName();

    private List<User> mFriends;
    private List<User> mIncomingRequests;
    private List<User> mOutgoingRequests;

    private Thread mFriendsThread;

    public FriendsList(){
        mFriends = new ArrayList<>();
        mIncomingRequests = new ArrayList<>();
        mOutgoingRequests = new ArrayList<>();
        // TODO : get friends from server.
        // TODO : store and only update.
        mFriendsThread = getFriendsThread();
        mFriendsThread.start();
    }


    public boolean isAFriend(User user){
        for(User friend : mFriends){
            if(friend.equals(user)){
                return true;
            }
        }
        return false;
    }

    private Thread getFriendsThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {

                Log.d(LOG_TAG, "Sent GET to /user/friends");
                HttpClient client = HttpClient.getInstance();
                String URL = NetworkUtilities.HTTP_URL + "/user/friends";

                Response response;
                try {
                    response = client.get(URL);
                } catch (IOException e){
                    e.printStackTrace();
                    Log.d(LOG_TAG, "Request Failed");
                    return;
                }

                Log.d(LOG_TAG , "Done Sending");
                Log.d(LOG_TAG , response.toString());
                JSONObject body;
                try {
                    body = new JSONObject(response.body().string());

                    JSONArray friendsArray = body.getJSONArray("friends");
                    JSONArray inReqArray = body.getJSONArray("incomingRequests");
                    JSONArray outReqAr = body.getJSONArray("outgoingRequests");

                    for(int i = 0; i < friendsArray.length(); i++){
                        mFriends.add(new User(UUID.fromString(friendsArray.getString(i))));
                    }
                    for(int i = 0; i < inReqArray.length(); i++){
                        mIncomingRequests.add(new User(UUID.fromString(inReqArray.getString(i))));
                    }
                    for(int i = 0; i < outReqAr.length(); i++){
                        mOutgoingRequests.add(new User(UUID.fromString(outReqAr.getString(i))));
                    }
                } catch (IOException e){
                    // TODO : handle
                    e.printStackTrace();
                    return;
                } catch (JSONException e){
                    int dles = 0;
                    e.printStackTrace();
                    return;
                }


            }
        });
    }


}
