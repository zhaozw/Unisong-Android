package io.unisong.android.activity.session;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.squareup.picasso.Picasso;
import com.thedazzler.droidicon.IconicFontDrawable;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.unisong.android.R;
import io.unisong.android.activity.musicplayer.tabs.SlidingTabLayout;
import io.unisong.android.activity.session.invite.InviteMemberActivity;
import io.unisong.android.activity.session.musicselect.MusicSelectActivity;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.song.Song;
import io.unisong.android.network.ntp.TimeManager;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;
import io.unisong.android.network.user.UserUtils;

/**
 * The main activity for an UnisongSession. Has the fragments for SessionMembers and
 * Created by Ethan on 9/26/2015.
 */
public class MainSessionActivity extends AppCompatActivity {

    // The message code for when we have been kicked out of a session
    public static final int KICKED = 23929;

    private static final String LOG_TAG = MainSessionActivity.class.getSimpleName();
    public final static String POSITION = "position";


    private UnisongSession session;
    private Song currentSong;
    private boolean mPlaying = false, footerOpen, seekThumbShowing = true;
    private Toolbar toolbar;
    private ViewPager pager;
    private SlidingTabLayout tabs;
    private SessionMessageHandler handler;

    private AudioStatePublisher publisher = AudioStatePublisher.getInstance();
    private TimeManager timeManager;
    private Button playPauseButton;
    private IconicFontDrawable playDrawable;
    private IconicFontDrawable pauseDrawable;
    private ScheduledThreadPoolExecutor executor;

    private RelativeLayout footer;
    private TextView footerSongName;
    private TextView footerSongArtist;
    private SeekBar footerSeekBar;
    private ImageView footerSongImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_main);

        toolbar = (Toolbar) findViewById(R.id.session_bar);
        setSupportActionBar(toolbar);

        Log.d(LOG_TAG, "Creating MainSessionActivity");
        // Configure the action bar.
        setSupportActionBar(toolbar);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowHomeEnabled(true);
            bar.setHomeButtonEnabled(true);
        }

        timeManager = TimeManager.getInstance();

        pager = (ViewPager) findViewById(R.id.session_pager);
        pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));

        tabs = (SlidingTabLayout) findViewById(R.id.session_tabs);
        tabs.setDistributeEvenly(true);
        tabs.setViewPager(pager);
        tabs.setSelectedIndicatorColors(ContextCompat.getColor(this, R.color.white));
        tabs.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor));

        playPauseButton = (Button) findViewById(R.id.play_pause_button);

        pauseDrawable = new IconicFontDrawable(this.getApplicationContext());
        pauseDrawable.setIcon("gmd-pause");
        pauseDrawable.setIconColor(ContextCompat.getColor(getApplicationContext(), R.color.black));

        playDrawable = new IconicFontDrawable(this.getApplicationContext());
        playDrawable.setIcon("gmd-play-arrow");
        playDrawable.setIconColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
        playPauseButton.setBackground(playDrawable);

        session = UnisongSession.getCurrentSession();

        handler = new SessionMessageHandler(this);
        session.setSessionActivityHandler(handler);

        // Close the footer
        footerOpen = false;

        footer = (RelativeLayout) findViewById(R.id.music_footer);
        footer.setVisibility(View.GONE);


        footerSongArtist = (TextView) footer.findViewById(R.id.playing_song_artist);
        footerSongName = (TextView) footer.findViewById(R.id.playing_song_name);
        footerSeekBar = (SeekBar) footer.findViewById(R.id.current_song_progress_bar);
        footerSeekBar.setEnabled(false);
        if (session != null && session.isMaster())
            footerSeekBar.setOnSeekBarChangeListener(mOnSeekChangeListener);
        footerSongImage = (ImageView) footer.findViewById(R.id.playing_song_image);

        // TODO : call a method on this activity when current song changes
        // TODO : call a method on this activity when we start playing.
        executor = new ScheduledThreadPoolExecutor(5);
        executor.scheduleAtFixedRate(this::updateFooter, 0, 100, TimeUnit.MILLISECONDS);
        if (session.isMaster() && publisher.getState() == AudioStatePublisher.PLAYING){
            footerSeekBar.setEnabled(true);
        } else if(!session.isMaster()){
            playPauseButton.setVisibility(View.GONE);
        }

        if(!session.isMaster() || publisher.getState() != AudioStatePublisher.PLAYING){
            footerSeekBar.getThumb().mutate().setAlpha(0);
            seekThumbShowing = false;
        }

    }

    /**
     * Updates the footer, opening, closing, and changing the progress bar as needed
     * Will also change the image + song name and artist to the appropriate one
     */
    private void updateFooter(){
        try {
            boolean selected = isASongSelected();

            int visibility = -1;

            if (!selected && footerOpen) visibility = View.GONE;

            if (selected && !footerOpen) visibility = View.VISIBLE;

            if (visibility == View.GONE) {
                runOnUiThread(() -> {
                    footer.setVisibility(View.GONE);
                    footerOpen = false;
                    if(publisher.getState() == AudioStatePublisher.IDLE)
                        playPauseButton.setBackground(playDrawable);
                });
            } else if(visibility == View.VISIBLE){
                runOnUiThread(() -> {
                    footer.setVisibility(View.VISIBLE);
                    footerOpen = true;
                });
            }

            if(!selected)
                return;

            boolean updateFooterInfo = false;


            if (!seekThumbShowing && session.isMaster() && publisher.getState() == AudioStatePublisher.PLAYING) {
                runOnUiThread(() -> {
                    footerSeekBar.getThumb().mutate().setAlpha(255);
                    if(!footerSeekBar.isEnabled()) {
                        footerSeekBar.setEnabled(true);
                    }
                });
                seekThumbShowing = true;
            } else if(seekThumbShowing && session.isMaster() && publisher.getState() != AudioStatePublisher.PLAYING){
                runOnUiThread(() -> {
                    footerSeekBar.getThumb().mutate().setAlpha(0);
                    if(footerSeekBar.isEnabled()) {
                        if(publisher.getState() != AudioStatePublisher.PAUSED)
                            footerSeekBar.setEnabled(false);
                    }
                });
                seekThumbShowing = false;
            }

            if(session.getCurrentSong() == null){
                Log.d(LOG_TAG , "Current song is null.");
            }
            if ((currentSong == null && session.getCurrentSong() != null) ||
                    (currentSong != null && session.getCurrentSong() != null &&  currentSong != session.getCurrentSong())) {
                currentSong = session.getCurrentSong();
                updateFooterInfo = true;
            }


            // If we need to, update the footer info
            if (updateFooterInfo) {

                String url = currentSong.getImageURL();

                if(url == null){
                    runOnUiThread(() -> {
                        footerSongImage.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.drawable.default_artwork));
                    });
                } else {

                    if (url.contains("http://")) {
                        runOnUiThread(() ->{
                            Picasso.with(getBaseContext()).load(currentSong.getImageURL()).into(footerSongImage);
                        });
                        // TODO : method call should happen from main thread?
                    } else {
                        runOnUiThread(() -> {
                            Picasso.with(getBaseContext()).load(new File(currentSong.getImageURL())).into(footerSongImage);
                        });
                    }
                }

                runOnUiThread(() -> {
                    footerSongArtist.setText(currentSong.getArtist());
                    footerSongName.setText(currentSong.getName());
                });
            }

            if(currentSong == null)
                return;

            // set the progress
            int progress = footerSeekBar.getProgress();
            int newProgress = timeManager.getProgress();

            if(newProgress != progress)
                footerSeekBar.setProgress(newProgress);

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

    private SeekBar.OnSeekBarChangeListener mOnSeekChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser)
                Log.d(LOG_TAG , "New Progress: " + progress);
//            if(progress)
//            seekBar.setProgress(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(currentSong == null || publisher.getState() == AudioStatePublisher.IDLE)
                return;

            long seekTime = (long)((currentSong.getDuration() / 1000) * (seekBar.getProgress() / 100.0));
            Log.d(LOG_TAG, "Progress is : " + footerSeekBar.getProgress() + " and we are seeking to : " + seekTime);
            if(session.isMaster())
                publisher.seek(seekTime);
        }
    };
    /**
     * Detects whether a current song is selected
     * @return - true if a song is selected, false if not
     */
    private boolean isASongSelected(){
        return session != null && session.getCurrentSong() != null;
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
            inviteFriend();
            return true;
        } else if(id == R.id.action_leave_session){
            onBackPressed();
            CurrentUser.leaveSession();
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

    /**
     * The FAB click listener for the SessionMembers tab
     */
    public void inviteFriendClick(View view){
        inviteFriend();
    }

    /**
     * The function that opens up a new InviteMemberActivity
     */
    public void inviteFriend(){
        Intent intent = new Intent(getApplicationContext() , InviteMemberActivity.class);
        startActivity(intent);
    }

    /**
     * The FAB click for the SesionSongsActivity
     * @param view
     */
    public void addSong(View view){
        Intent intent = new Intent(getApplicationContext(), MusicSelectActivity.class);
        startActivity(intent);
    }

    /**
     * This is the onClick method for the play/pause button
     * @param view
     */
    public void playPause(View view){
        AudioStatePublisher publisher = AudioStatePublisher.getInstance();

        if(publisher.getState() == AudioStatePublisher.PLAYING){
            playPauseButton.setBackground(playDrawable);
            publisher.pause();
        } else if(publisher.getState() == AudioStatePublisher.PAUSED){
            playPauseButton.setBackground(pauseDrawable);
            publisher.resume(publisher.getPausedTime());
        } else if(publisher.getState() == AudioStatePublisher.IDLE && session.getCurrentSong() != null){
            playPauseButton.setBackground(pauseDrawable);
            publisher.startSong();
        }

    }

    /**
     * The onClick listeners for the remove user button
     * @param view
     */
    public void kickUser(View view){

        try{
            UUID uuid = (UUID) view.getTag();

            confirmKick(uuid);
        } catch (NullPointerException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "NullPointerException in kickUser!");
        } catch (ClassCastException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "Tag casting failed in kickUser");
        }
    }

    /**
     * Launches a MaterialDialog confirming that a user should be kicked
     * @param uuid
     */
    private void confirmKick(UUID uuid){
        try {
            String kickMessage = getResources().getString(R.string.kick_message);

            User user = UserUtils.getUser(uuid);

            kickMessage = kickMessage.replace("USER_NAME" , user.getName());
            new MaterialDialog.Builder(this)
                    .content(kickMessage)
                    .positiveText(R.string.kick)
                    .negativeText(R.string.cancel)
                    .theme(Theme.LIGHT)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            UnisongSession session = UnisongSession.getCurrentSession();

                            if(session != null && session.isMaster()){
                                session.kick(user);
                            }
                        }
                    })
                    .show();
        } catch (NullPointerException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "User retrieval failed!");
        } catch (Resources.NotFoundException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "kickMessage not found!");
        }
    }

    /**
     * Displays a dialog notifying the user that they have been kicked.
     * When they press the 'Sorry' button, they are taken back to the main page
     * NOTE : they can still re-join
     */
    private void kicked(){
        try {
            String kickedMessage = getResources().getString(R.string.kicked_message);
            new MaterialDialog.Builder(this)
                    .content(kickedMessage)
                    .positiveText(R.string.sorry)
                    .theme(Theme.LIGHT)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            onBackPressed();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog){
                            onBackPressed();
                        }
                    })
            .show();
        } catch (Resources.NotFoundException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "kicked_message not found! Please rename something if you change it.");
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        session.setSessionActivityHandler(null);
    }

    public static class SessionMessageHandler extends Handler{

        private MainSessionActivity mActivity;
        public SessionMessageHandler(MainSessionActivity activity){
            super();
            mActivity = activity;
        }


        @Override
        public void handleMessage(Message message) {

            switch (message.what){
                case KICKED:
                    mActivity.runOnUiThread(() ->{
                        mActivity.kicked();
                    });
                    break;

            }
        }
    }

}