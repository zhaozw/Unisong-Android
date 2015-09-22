package io.unisong.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import de.hdodenhof.circleimageview.CircleImageView;
import io.unisong.android.R;
import io.unisong.android.activity.Friends.FriendsAdapter;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.FriendsList;
import io.unisong.android.network.user.User;

/**
 * Created by Ethan on 9/21/2015.
 */
public class UnisongActivity extends AppCompatActivity {

    private final static String LOG_TAG = UnisongActivity.class.getSimpleName();

    private Handler mHandler;
    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private FriendsAdapter mAdapter;
    private FriendsList mFriendsList;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unisong);

        mRecyclerView = (RecyclerView) findViewById(io.unisong.android.R.id.friendsRecyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mFriendsList = FriendsList.getInstance();

        // specify an adapter (see also next example)
        mAdapter = new FriendsAdapter(mFriendsList.getFriends());
        mRecyclerView.setAdapter(mAdapter);

        Log.d(LOG_TAG, "Starting thread");

        mToolbar = (Toolbar) findViewById(io.unisong.android.R.id.music_bar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setHomeButtonEnabled(true);

        User currentUser = CurrentUser.getInstance();

        mHandler = new Handler();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(io.unisong.android.R.menu.music_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if(id == R.id.action_settings){
            Toast.makeText(this, "Hey, you just hit the button! ", Toast.LENGTH_SHORT).show();
            return true;
        } else if(id == R.id.action_add_friend){
            Toast.makeText(this, "Hey, you just hit the button! ", Toast.LENGTH_SHORT).show();
            return true;
        } else if(id == R.id.action_log_out){
            new Thread(mLogoutRunnable).start();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Runnable mLogoutRunnable = new Runnable() {
        @Override
        public void run() {
            CurrentUser.logOut();
            runOnUiThread(mBackToLoginActivityRunnable);
        }
    };

    private Runnable mBackToLoginActivityRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }
    };

}
