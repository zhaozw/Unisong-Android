package io.unisong.android.activity.session;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.thedazzler.droidicon.IconicFontDrawable;

import java.io.File;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.unisong.android.R;
import io.unisong.android.activity.session.invite.InviteMemberActivity;
import io.unisong.android.activity.session.musicselect.MusicSelectActivity;
import io.unisong.android.activity.musicplayer.tabs.SlidingTabLayout;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.network.TimeManager;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.song.Song;

/**
 * Created by Ethan on 9/26/2015.
 */
public class MainSessionActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainSessionActivity.class.getSimpleName();
    public final static String POSITION = "position";


    private UnisongSession mSession;
    private Song mCurrentSong;
    private boolean mPlaying = false, mFooterOpen;
    private Toolbar mToolbar;
    private ViewPager mPager;
    private SlidingTabLayout mTabs;
    private Handler mHandler;

    private Button mPlayPauseButton;
    private IconicFontDrawable mPlayDrawable;
    private IconicFontDrawable mPauseDrawable;
    private ScheduledThreadPoolExecutor mExecutor;

    private RelativeLayout mFooter;
    private TextView mFooterSongName;
    private TextView mFooterSongArtist;
    private ProgressBar mFooterProgressBar;
    private ImageView mFooterSongImage;

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

        mPlayPauseButton = (Button) findViewById(R.id.play_pause_button);

        mPauseDrawable = new IconicFontDrawable(this.getApplicationContext());
        mPauseDrawable.setIcon("gmd-pause");
        mPauseDrawable.setIconColor(ContextCompat.getColor(getApplicationContext(), R.color.black));

        mPlayDrawable = new IconicFontDrawable(this.getApplicationContext());
        mPlayDrawable.setIcon("gmd-play-arrow");
        mPlayDrawable.setIconColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
        mPlayPauseButton.setBackground(mPlayDrawable);

        mSession = UnisongSession.getCurrentSession();

        mHandler = new Handler();

        // Close the footer
        mFooterOpen = false;

        mFooter = (RelativeLayout) findViewById(R.id.music_footer);
        mFooter.setVisibility(View.GONE);


        mFooterSongArtist = (TextView) mFooter.findViewById(R.id.playing_song_artist);
        mFooterSongName = (TextView) mFooter.findViewById(R.id.playing_song_name);
        mFooterProgressBar = (ProgressBar) mFooter.findViewById(R.id.current_song_progress_bar);
        mFooterSongImage = (ImageView) mFooter.findViewById(R.id.playing_song_image);

        // TODO : call a method on this activity when current song changes
        // TODO : call a method on this activity when we start playing.
        mExecutor = new ScheduledThreadPoolExecutor(5);
        mExecutor.scheduleAtFixedRate(this::updateFooter, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Updates the footer, opening, closing, and changing the progress bar as needed
     * Will also change the image + song name and artist to the appropriate one
     */
    private void updateFooter(){
        try {
            boolean selected = isASongSelected();

            int visibility = -1;

            if (!selected && mFooterOpen) visibility = View.GONE;

            if (selected && !mFooterOpen) visibility = View.VISIBLE;

            if (visibility != -1)
                mFooter.setVisibility(visibility);

            if(!selected)
                return;

            boolean updateFooterInfo = false;

            if ((mCurrentSong == null && mSession.getCurrentSong() != null) ||
                    (mCurrentSong != null && mCurrentSong != mSession.getCurrentSong())) {
                mCurrentSong = mSession.getCurrentSong();
                updateFooterInfo = true;
            }


            // If we need to, update the footer info
            if (updateFooterInfo) {

                String url = mCurrentSong.getImageURL();

                if(url == null){
                    runOnUiThread(() -> {
                        mFooterSongImage.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.drawable.default_artwork));
                    });
                } else {

                    if (url.contains("http://")) {
                        runOnUiThread(() ->{
                            Picasso.with(getBaseContext()).load(mCurrentSong.getImageURL()).into(mFooterSongImage);
                        });
                        // TODO : method call should happen from main thread?
                    } else {
                        runOnUiThread(() -> {
                            Picasso.with(getBaseContext()).load(new File(mCurrentSong.getImageURL())).into(mFooterSongImage);
                        });
                    }
                }

                runOnUiThread(() -> {
                    mFooterSongArtist.setText(mCurrentSong.getArtist());
                    mFooterSongName.setText(mCurrentSong.getName());
                });
            }


            AudioStatePublisher publisher = AudioStatePublisher.getInstance();
            // set the progress
            int progress = mFooterProgressBar.getProgress();
            long duration = mCurrentSong.getDuration();
            int newProgress = progress;
            long timePlayed = -1;

            if(publisher.getState() == AudioStatePublisher.PLAYING){
                timePlayed = System.currentTimeMillis() - TimeManager.getInstance().getSongStartTime();
            } else if(publisher.getState() == AudioStatePublisher.PAUSED){
                timePlayed = publisher.getResumeTime();
            }

            if(timePlayed != -1) {
                newProgress = (int) ((duration / (double) timePlayed) * 100);
            } else {
                newProgress = 100;
            }

            if(progress != newProgress)
                mFooterProgressBar.setProgress(newProgress);

        } catch (NullPointerException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "NullPointerException called at some point in updateFooter!");
        } catch (ClassCastException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "ClassCastException in updateFooter! Did we update a view and forget to change it in here?");
        } catch (Exception e){
            e.printStackTrace();
            Log.d(LOG_TAG , "Uncaught exception! This should not happen!");
        }

    }

    /**
     * Detects whether a current song is selected
     * @return - true if a song is selected, false if not
     */
    private boolean isASongSelected(){
        if(mSession.getCurrentSong() != null)
            return true;

        return false;
    }

    private void updateProgress(){

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

    public void onFABClick(View view){
        Intent intent = new Intent(getApplicationContext() , InviteMemberActivity.class);
        startActivity(intent);
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

    public void playPause(View view){

        AudioStatePublisher publisher = AudioStatePublisher.getInstance();

        publisher.play();

        if(mPlaying){
            mPlayPauseButton.setBackground(mPlayDrawable);
        } else {
            mPlayPauseButton.setBackground(mPauseDrawable);
        }

        mPlaying = !mPlaying;


    }

}