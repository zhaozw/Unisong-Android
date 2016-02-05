package io.unisong.android.activity.session.invite;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import io.unisong.android.R;
import io.unisong.android.activity.friends.FriendsAdapter;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.FriendsList;
import io.unisong.android.network.user.User;

/**
 * This activity is used to select the member to invite to a SongSession
 * Created by ezturner on 1/13/2016.
 */
public class InviteMemberActivity extends AppCompatActivity{


    private static final String LOG_TAG = InviteMemberActivity.class.getSimpleName();


    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private FriendsAdapter adapter;
    private Toolbar toolbar;
    private FriendsList friendsList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_member);


        toolbar = (Toolbar) findViewById(R.id.session_bar);

        // Configure the action bar.
        setSupportActionBar(toolbar);
        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setDisplayShowHomeEnabled(true);
            bar.setHomeButtonEnabled(true);
        }

        friendsList = FriendsList.getInstance();

        if(friendsList != null){

            recyclerView = (RecyclerView) findViewById(R.id.friends_recyclerview);

            // use this setting to improve performance if you know that changes
            // in content do not change the mLayout size of the RecyclerView
            recyclerView.setHasFixedSize(true);

            // use a linear layoutManager
            layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);

            adapter = new FriendsAdapter(friendsList.getFriends());
            recyclerView.setAdapter(adapter);
        } else {
            Log.d(LOG_TAG, "FriendsList was null! It should not be!");
            onBackPressed();
        }
    }

    public void onFriendClick(View view){
        try {
            String inviteMessage = " has invited you to their session!";

            UnisongSession session = UnisongSession.getCurrentSession();
            User user = CurrentUser.getInstance();

            inviteMessage = user.getName() + inviteMessage;


            UUID uuid = (UUID) view.getTag();

            JSONObject object = new JSONObject();
            Log.d(LOG_TAG , "UUID : " + uuid);

            object.put("userIDString", uuid.toString());
            object.put("message", inviteMessage);
            object.put("sessionID", session.getSessionID());

            SocketIOClient client = SocketIOClient.getInstance();

            client.emit("invite user" , object);

        } catch (NullPointerException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "is CurrentUser or CurrentSession null?");
        } catch (ClassCastException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "View tag was cast incorrectly!");
        } catch (JSONException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "Creation of JSON Object failed for inviteFriend() !");
        }
    }
}
