package io.unisong.android.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import io.unisong.android.R;
import io.unisong.android.activity.Friends.FriendsAdapter;
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

        mToolbar = (Toolbar) findViewById(io.unisong.android.R.id.music_bar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setHomeButtonEnabled(true);

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

        //findViewById(R.id.unisong_first_divider).setAlpha(0.12f);

    }

    public void onProfileClick(View view){
        Log.d(LOG_TAG , "User Profile onClick Received!");
        new MaterialDialog.Builder(getApplicationContext())
                .title(R.string.change_profile_picture)
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
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

    public void sayhi(View v){
        Log.d(LOG_TAG , "Hiiiiiii182738162487");
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

            while(user == null || user.doesNotHaveProfilePicture()){
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
        protected void onPostExecute(User user){

            TextView name = (TextView) findViewById(R.id.current_user_name);
            name.setText(user.getName());

            TextView username = (TextView) findViewById(R.id.current_user_username);
            username.setText("@" + user.getUsername());

            CircleImageView imageView = (CircleImageView) findViewById(R.id.user_image);

            Bitmap profile = user.getProfilePicture();

            if(profile != null) {
                imageView.setImageBitmap(profile);
            }
        }
    }
}
