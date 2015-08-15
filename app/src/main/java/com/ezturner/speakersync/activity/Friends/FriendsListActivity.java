package com.ezturner.speakersync.activity.Friends;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.ezturner.speakersync.R;
import com.ezturner.speakersync.activity.MusicPlayer.MusicPlayer;
import com.ezturner.speakersync.activity.MusicPlayer.MusicPlaying;
import com.ezturner.speakersync.activity.MusicPlayer.Tabs.SlidingTabLayout;
import com.ezturner.speakersync.activity.NavigationDrawerFragment;
import com.ezturner.speakersync.network.HttpClient;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by ezturner on 8/11/2015.
 */
public class FriendsListActivity  extends ActionBarActivity implements NavigationDrawerFragment.OnFragmentInteractionListener {

    private final static String LOG_TAG = FriendsListActivity.class.getSimpleName();

    private Thread mFriendsThread;
    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private FriendsAdapter mAdapter;


    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        mRecyclerView = (RecyclerView) findViewById(R.id.friendsRecyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        ArrayList<String> myDataset = new ArrayList<>();
        myDataset.add("Ethan");
        myDataset.add("Swahg");
        myDataset.add("Ethan");
        myDataset.add("Swahg");
        myDataset.add("Ethan");
        myDataset.add("Swahg");
        myDataset.add("Ethan");
        myDataset.add("Swahg");

        // specify an adapter (see also next example)
        mAdapter = new FriendsAdapter(myDataset);
        mRecyclerView.setAdapter(mAdapter);

        Log.d(LOG_TAG , "Starting thread");
        mFriendsThread = getFriendsThread();
        mFriendsThread.start();


        mToolbar = (Toolbar) findViewById(R.id.music_bar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setHomeButtonEnabled(true);

        NavigationDrawerFragment drawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);

        drawerFragment.setUp((DrawerLayout)findViewById(R.id.drawer_layout) , mToolbar , R.id.fragment_navigation_drawer);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.music_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if(id == R.id.action_settings){
            Toast.makeText(this, "Hey, you just hit the button! ", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private Thread getFriendsThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {

                Log.d(LOG_TAG , "Sent GET to /user/friends");
                HttpClient client = HttpClient.getInstance();
                String URL = NetworkUtilities.EC2_INSTANCE + "/user/friends";

                Response response;
                try {
                    response = client.get(URL);
                } catch (IOException e){
                    e.printStackTrace();
                    Log.d(LOG_TAG, "Request Failed");
                    return;
                }
            }
        });
    }

    public void onClick(View v){

    }

    @Override
    public void onFragmentInteraction(Uri uri){
        //TODO: Make this do something
    }

    public void onDrawerClick(View v){
        //TODO: add ripple effect on this click
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawers();
        //TODO: Make a settings screen so this does something
        if(v.findViewById(R.id.drawerRowImage).getTag().equals(1)){
//            TODO:
        }
    }

    public void friendRowClick(View v){
        Log.d(LOG_TAG, "Friend Row Clicked!");
        getFriendsThread().start();
    }


}
