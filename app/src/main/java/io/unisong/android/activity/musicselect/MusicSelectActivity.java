package io.unisong.android.activity.musicselect;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.unisong.android.R;
import io.unisong.android.activity.musicplayer.tabs.SlidingTabLayout;
import io.unisong.android.activity.session.SessionSongsAdapter;
import io.unisong.android.network.host.Broadcaster;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.song.LocalSong;
import io.unisong.android.network.user.CurrentUser;

/**
 * Created by Ethan on 2/26/2015.
 */
public class MusicSelectActivity extends AppCompatActivity{

    private final static String LOG_TAG = MusicSelectActivity.class.getSimpleName();



    public final static String POSITION = "position";
    private Toolbar mToolbar;

    private ViewPager mPager;
    private SlidingTabLayout mTabs;
    private MusicAdapter mAlphabeticalAdapter;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_select);

        mToolbar = (Toolbar) findViewById(R.id.music_bar);

        setSupportActionBar(mToolbar);
        // get and configure actionbar
        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setDisplayShowHomeEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setHomeButtonEnabled(true);
        }



        mPager = (ViewPager) findViewById(R.id.player_pager);
        mPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));

        mTabs = (SlidingTabLayout) findViewById(R.id.player_tabs);
        mTabs.setViewPager(mPager);
        mTabs.setSelectedIndicatorColors(ContextCompat.getColor(this, R.color.white));
        mTabs.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor));
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
        } if(id == android.R.id.home){
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    public void addItem(View v){
        String tag = (String)v.getTag();
        String[] parts = tag.split(":");

        long ID = Long.parseLong(parts[1]);

        Log.d(LOG_TAG , "Song chosen");
        MusicDataManager manager = MusicDataManager.getInstance();
        UISong uiSong = manager.getSongByID(ID);
        LocalSong song = new LocalSong(uiSong);
        UnisongSession session = CurrentUser.getInstance().getSession();
        if(session != null)
            session.addSong(song);
        onBackPressed();
    }


    public void onRowClick(View v){
        String tag = (String)v.getTag();
        String[] parts = tag.split(":");

        int type = Integer.parseInt(parts[0]);

        long ID = Long.parseLong(parts[1]);

        switch (type){
            case MusicData.ALBUM:

                break;

            case MusicData.GENRE:

                break;

            case MusicData.PLAYLIST:

                break;

            case MusicData.ARTIST:

                break;

            case MusicData.SONG:
                Log.d(LOG_TAG , "Song chosen");
                MusicDataManager manager = MusicDataManager.getInstance();
                UISong uiSong = manager.getSongByID(ID);
                LocalSong song = new LocalSong(uiSong);
                UnisongSession session = CurrentUser.getInstance().getSession();
                if(session != null)
                    session.addSong(song);
                onBackPressed();
                break;
        }
    }


    class MyPagerAdapter extends FragmentPagerAdapter {

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
            return mTabNames.length;
        }
    }

    public static class MyFragment extends Fragment{
        private RecyclerView mMusicDataRecyclerView;
        private MusicAdapter mAdapter;
        private LinearLayoutManager mLayoutManager;
        private MusicDataManager mDataManager;

        public static MyFragment getInstance(int position){

            MyFragment myFragment = new MyFragment();

            Bundle args = new Bundle();
            args.putInt(POSITION , position );

            myFragment.setArguments(args);

            return myFragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
            View layout = inflater.inflate(R.layout.fragment_music_display, container , false);

            mDataManager = MusicDataManager.getInstance();
            mMusicDataRecyclerView = (RecyclerView) layout.findViewById(R.id.music_recycler_view);

            // use a linear mLayout manager
            mLayoutManager = new LinearLayoutManager(getContext());
            mMusicDataRecyclerView.setLayoutManager(mLayoutManager);

            mAdapter = new MusicAdapter(layout.getContext());
            mAdapter.setData(mDataManager.getData(getArguments().getInt(POSITION)));
            mMusicDataRecyclerView.setAdapter(mAdapter);

            return layout;
        }
    }

}
