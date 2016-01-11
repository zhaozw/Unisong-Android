package io.unisong.android.activity.session;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import io.unisong.android.R;
import io.unisong.android.activity.musicselect.MusicSelectActivity;
import io.unisong.android.activity.musicplayer.tabs.SlidingTabLayout;
import io.unisong.android.network.session.UnisongSession;

/**
 * Created by Ethan on 9/26/2015.
 */
public class MainSessionActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainSessionActivity.class.getSimpleName();
    public final static String POSITION = "position";


    private Toolbar mToolbar;
    private ViewPager mPager;
    private SlidingTabLayout mTabs;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_main);

        mToolbar = (Toolbar) findViewById(R.id.session_bar);

        // Configure the action bar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mPager = (ViewPager) findViewById(R.id.session_pager);
        mPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));

        mTabs = (SlidingTabLayout) findViewById(R.id.session_tabs);
        mTabs.setDistributeEvenly(true);
        mTabs.setViewPager(mPager);
        mTabs.setSelectedIndicatorColors(ContextCompat.getColor(this, R.color.white));
        mTabs.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_session_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if(id == R.id.action_settings){
            Toast.makeText(this, "Hey, you just hit the button! ", Toast.LENGTH_SHORT).show();
            return true;
        } else if(id == R.id.action_invite_friend){
            Toast.makeText(this, "Invite a friend!", Toast.LENGTH_SHORT).show();
            return true;
        } else if(id == R.id.action_leave_session){
            UnisongSession.getCurrentSession().leave();
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    class MyPagerAdapter extends FragmentPagerAdapter {
        SessionMembersFragment mFriendsFragment;
        SessionSongsFragment mSongsFragment;

        String[] mTabNames;
        public MyPagerAdapter(FragmentManager fragmentManager){
            super(fragmentManager);
            mTabNames = getResources().getStringArray(R.array.unisong_session_tab_names);
            mFriendsFragment = new SessionMembersFragment();
            mSongsFragment = new SessionSongsFragment();
        }

        @Override
        public CharSequence getPageTitle(int position){
            if(position >=0 && position < mTabNames.length) {
                return mTabNames[position];
            } else {
                return "404_TITLE";
            }
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0){
                return new SessionMembersFragment();
                // TODO : return SessionMembersFragment
            } else if(position == 1){
                return new SessionSongsFragment();
                // TODO : return SessionSongsFragment
            }

            return new Fragment();
        }

        @Override
        public int getCount() {
            return mTabNames.length;
        }
    }

    public void inviteFriend(View view){
        Toast.makeText(this, "Invite a friend!", Toast.LENGTH_SHORT).show();

    }

    public void addSong(View view){
        Intent intent = new Intent(getApplicationContext(), MusicSelectActivity.class);
        startActivity(intent);
    }

    public void removeSong(View view){
        UnisongSession session = UnisongSession.getCurrentSession();

        try {
            int songID = Integer.parseInt((String)view.getTag());
            Log.d(LOG_TAG , "ID : " + getResources().getResourceEntryName(view.getId()));
            Log.d(LOG_TAG , "Tag : " +  view.getTag());
            Log.d(LOG_TAG, "SongID : " + songID);
            session.deleteSong(songID);
        } catch (Exception e){
            // Catch NullPointerException and cast exception
            e.printStackTrace();
        }
    }
}
