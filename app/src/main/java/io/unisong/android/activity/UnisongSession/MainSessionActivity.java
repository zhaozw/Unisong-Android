package io.unisong.android.activity.unisongsession;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import io.unisong.android.R;
import io.unisong.android.activity.musicplayer.Tabs.SlidingTabLayout;

/**
 * Created by Ethan on 9/26/2015.
 */
public class MainSessionActivity extends AppCompatActivity {

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
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        mPager = (ViewPager) findViewById(R.id.player_pager);
        mPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));

        mTabs = (SlidingTabLayout) findViewById(R.id.player_tabs);
        mTabs.setViewPager(mPager);
        mTabs.setSelectedIndicatorColors(getResources().getColor(R.color.colorAccent));
        mTabs.setBackgroundColor(getResources().getColor(R.color.primaryColor));
    }

    class MyPagerAdapter extends FragmentPagerAdapter {

        String[] mTabNames;
        public MyPagerAdapter(FragmentManager fragmentManager){
            super(fragmentManager);
            mTabNames = getResources().getStringArray(R.array.unisong_session_tab_names);
        }

        @Override
        public CharSequence getPageTitle(int position){
            return mTabNames[position];

//            if(position >=0 && position < mTabNames.length) {
//                return mTabNames[position];
//            } else {
//                return "404_TITLE";
//            }
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0){
                // TODO : return SessionFriendsFragment
            } else if(position == 1){
                // TODO : return SessionSongsFragment
            }

            return new Fragment();
        }

        @Override
        public int getCount() {
            return 5;
        }
    }
}
