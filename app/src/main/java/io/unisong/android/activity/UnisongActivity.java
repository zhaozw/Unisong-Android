package io.unisong.android.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;
import com.thedazzler.droidicon.IconicFontDrawable;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import io.unisong.android.PrefUtils;
import io.unisong.android.R;
import io.unisong.android.activity.friends.FriendsAdapter;
import io.unisong.android.activity.session.MainSessionActivity;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.FriendsList;
import io.unisong.android.network.user.ImageUtilities;
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
    private RelativeLayout mUserProfileLayout;
    private CircleImageView mUserProfileImageView;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unisong);

        mRecyclerView = (RecyclerView) findViewById(R.id.friends_recyclerview);

        // use this setting to improve performance if you know that changes
        // in content do not change the mLayout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear mLayout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mFriendsList = FriendsList.getInstance();

        // specify an adapter (see also next example)
        mAdapter = new FriendsAdapter(mFriendsList.getFriends());
        mRecyclerView.setAdapter(mAdapter);

        Log.d(LOG_TAG, "Starting thread");

        User currentUser = CurrentUser.getInstance();

        User user = CurrentUser.getInstance();
        // TODO : add a handler/something that runs on the UI thread to load names
        if(user != null) {
            TextView name = (TextView) findViewById(R.id.current_user_name);
            name.setText(user.getName());

            TextView username = (TextView) findViewById(R.id.current_user_username);
            username.setText("@" + user.getUsername());
        }



        new LoadCurrentUserProfile().execute();

        mHandler = new Handler();

        mUserProfileImageView = (CircleImageView) findViewById(R.id.user_image);

        if(PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "unisong").equals("unisong")){
            mUserProfileImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onProfileClick(view);
                }
            });
        }

        //findViewById(R.id.unisong_first_divider).setAlpha(0.12f);

        Button moreButton = (Button) findViewById(R.id.settings_button);

        IconicFontDrawable iconicFontDrawable = new IconicFontDrawable(this.getApplicationContext());
        iconicFontDrawable.setIcon("gmd-more-vert");
        iconicFontDrawable.setIconColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        iconicFontDrawable.setIconPadding(24);

        moreButton.setBackground(iconicFontDrawable);

        Button logoutButton = (Button) findViewById(R.id.logout_button);

        iconicFontDrawable = new IconicFontDrawable(this.getApplicationContext());
        iconicFontDrawable.setIcon("gmd-exit-to-app");
        iconicFontDrawable.setIconColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        iconicFontDrawable.setIconPadding(22);

        logoutButton.setBackground(iconicFontDrawable);

        Button addFriendButton = (Button) findViewById(R.id.add_friend_button);

        iconicFontDrawable = new IconicFontDrawable(this.getApplicationContext());
        iconicFontDrawable.setIcon("entypo-add-user");
        iconicFontDrawable.setIconColor(ContextCompat.getColor(getApplicationContext(), R.color.secondaryText));
        iconicFontDrawable.setIconPadding(16);

        addFriendButton.setBackground(iconicFontDrawable);

    }

    public void onProfileClick(View view){
        Log.d(LOG_TAG, "User Profile onClick Received!");
        // TODO : set the colors for the text.
        new MaterialDialog.Builder(this)
                .content(R.string.change_profile_picture)
                .positiveText(R.string.change)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        photoPickerIntent.setType("image/*");
                        startActivityForResult(photoPickerIntent, 1);
                    }
                })
                .show();
    }

    private AlphaAnimation buttonClick = new AlphaAnimation(0.2F, 1F);

    public void settingsClick(View view){
        Log.d(LOG_TAG, "Settings Click Received");
        buttonClick.setDuration(1000);
        view.startAnimation(buttonClick);
        // TODO : adjust the menu location so that we're not below the settings and instead are on top of it
        // TODO : also add dividers.
        PopupMenu popupMenu = new PopupMenu(this , view);
        popupMenu.inflate(R.menu.menu_unisong_options);
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
    }

    public void logout(View v){
        buttonClick.setDuration(1000);
        v.startAnimation(buttonClick);
        // TODO : have a MaterialDialog pop up for confirmation
        new Thread(mLogoutRunnable).start();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            Uri chosenImageUri = data.getData();

            Log.d(LOG_TAG , chosenImageUri.toString());

            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), chosenImageUri);
                Matrix matrix = new Matrix();

                // TODO : see if we are rotating this correctly.
                matrix.postRotate(ImageUtilities.getImageRotation(getApplicationContext() , chosenImageUri));

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);

                mUserProfileImageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e){
                e.printStackTrace();
                return;
            } catch (IOException e){
                e.printStackTrace();
                return;
            }


            getUploadThread(bitmap).start();
        }
    }

    public void addFriendClick(View v){
        Log.d(LOG_TAG , "Add friend click");
        Intent intent = new Intent(getApplicationContext() , AddFriendActivity.class);
        startActivity(intent);
    }

    private Thread getUploadThread(final Bitmap bitmap){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                CurrentUser.getInstance().uploadProfilePicture(bitmap);
            }
        });
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
            // TODO : see if we need this?
            Toast.makeText(this, "Hey, you just hit the button! ", Toast.LENGTH_SHORT).show();
            return true;
        } else if(id == R.id.action_add_friend){
            Intent intent = new Intent(getApplicationContext() , AddFriendActivity.class);
            startActivity(intent);
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

    private class LoadCurrentUserProfile extends AsyncTask<Void , Void, User> {


        @Override
        protected User doInBackground(Void... voids) {

            User user = CurrentUser.getInstance();

            while(user == null || user.getName() == null){
                user = CurrentUser.getInstance();

                synchronized (this){
                    try {
                        this.wait(10);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }

            return user;
        }

        @Override
        protected void onPostExecute(User user) {
            Picasso.with(getApplicationContext()).load(user.getProfileURL()).into((ImageView) findViewById(R.id.user_image));

            Log.d(LOG_TAG , "Current User done loading profile picture, assigning to ImageView");
            TextView name = (TextView) findViewById(R.id.current_user_name);
            name.setText(user.getName());

            TextView username = (TextView) findViewById(R.id.current_user_username);
            username.setText("@" + user.getUsername());

        }
    }

    public void onFABClick(View view){
        UnisongSession session = UnisongSession.getInstance();
        if(session != null){
            // TODO : if we're in a session, move to the Session screen
        } else {
            // TODO : if not, then create a session

            Intent broadcast = new Intent("unisong-service-interface");
            // You can also include some extra data.
            broadcast.putExtra("command" , "start session");

            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);

            Intent intent = new Intent(getApplicationContext() , MainSessionActivity.class);
            startActivity(intent);
        }


    }
}
