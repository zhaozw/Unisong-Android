package io.unisong.android.activity.friends.friend_requests;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

import io.unisong.android.R;
import io.unisong.android.network.user.FriendsList;
import io.unisong.android.network.user.User;
import io.unisong.android.network.user.UserUtils;

/**
 * Created by Ethan on 2/16/2016.
 */
public class FriendRequestActivity extends AppCompatActivity {

    private static final String LOG_TAG = FriendRequestActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RecyclerView requestsView;
    private FriendsList friendsList;
    private FriendRequestAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);

        toolbar = (Toolbar) findViewById(R.id.friend_request_bar);
        setSupportActionBar(toolbar);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowHomeEnabled(true);
            bar.setHomeButtonEnabled(true);
        }

        FriendsList list = FriendsList.getInstance();
        adapter = new FriendRequestAdapter(list.getIncomingRequests());


        friendsList = FriendsList.getInstance();
        requestsView = (RecyclerView) findViewById(R.id.friend_requests_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getBaseContext());
        requestsView.setLayoutManager(layoutManager);
        requestsView.setAdapter(adapter);
    }


    public void acceptRequest(View view){
        try {
            final User user = (User) view.getTag();
            Callback callback = new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(() -> {
                        String failedAddFriendMessage = getResources().getString(R.string.add_friend_failed);
                        Toast toast = Toast.makeText(getApplicationContext() , failedAddFriendMessage , Toast.LENGTH_LONG);
                        toast.show();
                    });
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    if(response.code() == 200){
                        runOnUiThread(() -> {
                            String failedAddFriendMessage = getResources().getString(R.string.add_friend_succeeded);
                            Toast toast = Toast.makeText(getApplicationContext(), failedAddFriendMessage, Toast.LENGTH_LONG);
                            toast.show();
                            if(user != null)
                            adapter.remove(user);
                        });

                    } else {
                        runOnUiThread(() -> {
                            String failedAddFriendMessage = getResources().getString(R.string.add_friend_failed);
                            Toast toast = Toast.makeText(getApplicationContext(), failedAddFriendMessage, Toast.LENGTH_LONG);
                            toast.show();
                        });
                    }

                }
            };
            friendsList.addFriendToList(user, callback);
        } catch (ClassCastException e){
            e.printStackTrace();
            Log.d(LOG_TAG, "Tag casting failed in acceptRequest()");
        }
    }

    public void denyRequest(View view){
        try {
            final User user = (User) view.getTag();
            Callback callback = new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(() -> {
                        String failedAddFriendMessage = getResources().getString(R.string.deny_friend_failed);
                        Toast toast = Toast.makeText(getApplicationContext() , failedAddFriendMessage , Toast.LENGTH_LONG);
                        toast.show();
                    });
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    if(response.code() == 200){
                        runOnUiThread(() -> {
                            String failedAddFriendMessage = getResources().getString(R.string.deny_friend_succeeded);
                            Toast toast = Toast.makeText(getApplicationContext(), failedAddFriendMessage, Toast.LENGTH_LONG);
                            toast.show();
                            if(user != null)
                                adapter.remove(user);
                        });

                    } else {
                        runOnUiThread(() -> {
                            String failedAddFriendMessage = getResources().getString(R.string.deny_friend_failed);
                            Toast toast = Toast.makeText(getApplicationContext(), failedAddFriendMessage, Toast.LENGTH_LONG);
                            toast.show();
                        });
                    }

                }
            };
            friendsList.deleteFriend(user, callback);
        } catch (ClassCastException e){
            e.printStackTrace();
            Log.d(LOG_TAG, "Tag casting failed in acceptRequest()");
        }
    }


}
