package io.unisong.android.activity.MusicPlayer;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import io.unisong.android.activity.NavigationDrawerFragment;
import io.unisong.android.R;
import io.unisong.android.activity.MusicPlayer.Tabs.SlidingTabLayout;

/**
 * Created by ezturner on 4/8/2015.
 */
public class MusicPlayer extends ActionBarActivity implements NavigationDrawerFragment.OnFragmentInteractionListener{

    public final static String POSITION = "position";

    private Toolbar mToolbar;

    private ViewPager mPager;
    private SlidingTabLayout mTabs;


    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        mToolbar = (Toolbar) findViewById(R.id.music_bar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setHomeButtonEnabled(true);

        NavigationDrawerFragment drawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);

        drawerFragment.setUp((DrawerLayout)findViewById(R.id.drawer_layout) , mToolbar , R.id.fragment_navigation_drawer);


        mPager = (ViewPager) findViewById(R.id.player_pager);
        mPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));

        mTabs = (SlidingTabLayout) findViewById(R.id.player_tabs);
        mTabs.setViewPager(mPager);
        mTabs.setSelectedIndicatorColors(getResources().getColor(R.color.colorAccent));
        mTabs.setBackgroundColor(getResources().getColor(R.color.primaryColor));

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
            Toast.makeText(this , "Hey, you just hit the button! " , Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void onClick(View v){
        startActivity(new Intent(MusicPlayer.this , MusicPlaying.class));
    }


    @Override
    public void onFragmentInteraction(Uri uri) {
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

    class MyPagerAdapter extends FragmentPagerAdapter{

        String[] mTabNames;
        public MyPagerAdapter(FragmentManager fragmentManager){
            super(fragmentManager);
            mTabNames = getResources().getStringArray(R.array.music_player_tabs);
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
            MyFragment myFragment = MyFragment.getInstance(position);

            return myFragment;
        }

        @Override
        public int getCount() {
            return 5;
        }
    }

    public static class MyFragment extends Fragment{
        private TextView mTextView;

        public static MyFragment getInstance(int position){

            MyFragment myFragment = new MyFragment();

            Bundle args = new Bundle();
            args.putInt(POSITION , position );

            myFragment.setArguments(args);

            return myFragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
            View layout = inflater.inflate(R.layout.music_display, container , false);


            return layout;
        }
    }
}
