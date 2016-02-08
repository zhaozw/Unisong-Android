package io.unisong.android.activity.friends;

import android.net.Uri;
import android.os.Bundle;
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

import io.unisong.android.activity.NavigationDrawerFragment;
import io.unisong.android.network.user.FriendsList;

/**
 * Created by ezturner on 8/11/2015.
 */
public class FriendsListActivity  extends ActionBarActivity implements NavigationDrawerFragment.OnFragmentInteractionListener {

    private final static String LOG_TAG = FriendsListActivity.class.getSimpleName();

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private FriendsAdapter adapter;
    private FriendsList friendsList;


    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(io.unisong.android.R.layout.activity_friends);
        recyclerView = (RecyclerView) findViewById(io.unisong.android.R.id.friendsRecyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        friendsList = FriendsList.getInstance();

        // specify an adapter (see also next example)
        adapter = new FriendsAdapter(friendsList.getFriends());
        recyclerView.setAdapter(adapter);

        Log.d(LOG_TAG , "Starting thread");

        toolbar = (Toolbar) findViewById(io.unisong.android.R.id.music_bar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setHomeButtonEnabled(true);

        NavigationDrawerFragment drawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(io.unisong.android.R.id.fragment_navigation_drawer);

        drawerFragment.setUp((DrawerLayout)findViewById(io.unisong.android.R.id.drawer_layout) ,
                toolbar, io.unisong.android.R.id.fragment_navigation_drawer);
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

        if(id == io.unisong.android.R.id.action_settings){
            Toast.makeText(this, "Hey, you just hit the button! ", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClick(View v){

    }

    @Override
    public void onFragmentInteraction(Uri uri){
        //TODO: Make this do something
    }

    public void onDrawerClick(View v){
        //TODO: add ripple effect on this click
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(io.unisong.android.R.id.drawer_layout);
        drawerLayout.closeDrawers();
        //TODO: Make a settings screen so this does something
        if(v.findViewById(io.unisong.android.R.id.drawerRowImage).getTag().equals(3)){


        }

        if(v.findViewById(io.unisong.android.R.id.drawerRowImage).getTag().equals(1)){

        }
    }

    public void friendRowClick(View v){

    }


}
