package io.unisong.android.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
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
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.squareup.picasso.Picasso;
import com.thedazzler.droidicon.IconicFontDrawable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import io.unisong.android.PrefUtils;
import io.unisong.android.R;
import io.unisong.android.activity.friends.AddFriendActivity;
import io.unisong.android.activity.friends.FriendsAdapter;
import io.unisong.android.activity.friends.contacts.AddFriendsFromContactsActivity;
import io.unisong.android.activity.friends.facebook.AddFriendsFromFacebookActivity;
import io.unisong.android.activity.friends.username.AddFriendByUsernameActivity;
import io.unisong.android.activity.session.MainSessionActivity;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.session.SessionUtils;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.FriendsList;
import io.unisong.android.network.user.ImageUtilities;
import io.unisong.android.network.user.User;
import io.unisong.android.network.user.UserUtils;

/**
 * The main activity for the application, has friends list, adding friends, and creating sessions
 * Created by Ethan on 9/21/2015.
 */
public class UnisongActivity extends AppCompatActivity {

    public static final int INVITE = 241293;
    private final static String LOG_TAG = UnisongActivity.class.getSimpleName();

    private IncomingHandler handler;
    private Toolbar mToolbar;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private FriendsAdapter adapter;
    private FriendsList friendsList;
    private CircleImageView userProfileImageView;
    private Intent networkIntent;
    private Intent mediaIntent;
    

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unisong);

        recyclerView = (RecyclerView) findViewById(R.id.friends_recyclerview);

        // use this setting to improve performance if you know that changes
        // in content do not change the mLayout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear mLayout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        friendsList = FriendsList.getInstance();
        long currentTime = System.currentTimeMillis();
        boolean restartedServices = false;

        // TODO : ensure friendSList
        while(friendsList == null){

            try{
                synchronized (this){
                    this.wait(1);
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
            friendsList = FriendsList.getInstance();
        }


        // specify an adapter (see also next example)
        adapter = new FriendsAdapter(friendsList.getFriends());
        recyclerView.setAdapter(adapter);

        Log.d(LOG_TAG, "creating UnisongActivity");

        User currentUser = CurrentUser.getInstance();

        User user = CurrentUser.getInstance();
        // TODO : add a handler/something that runs on the UI thread to load names
        if(user != null) {
            TextView name = (TextView) findViewById(R.id.current_user_name);
            name.setText(user.getName());

            TextView username = (TextView) findViewById(R.id.current_user_username);
            username.setText("@" + user.getUsername());
        }

        UnisongSession session = UnisongSession.getCurrentSession();

        if(session != null){
            Log.d(LOG_TAG , "UnisongSession Loaded!");
            Intent intent = new Intent(getApplicationContext() , MainSessionActivity.class);
            startActivity(intent);
        } else {
            UnisongSession.notifyWhenLoaded(this);
            Log.d(LOG_TAG, "UnisongSession Not Loaded");
        }


        new LoadCurrentUserProfile().execute();

        handler = new IncomingHandler(this);
        SocketIOClient client = SocketIOClient.getInstance();

        if(!client.isConnected())
            client.connect();

        client.registerInviteHandler(handler);

        userProfileImageView = (CircleImageView) findViewById(R.id.user_image);

        if(PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "unisong").equals("unisong")){
            userProfileImageView.setOnClickListener(new View.OnClickListener() {
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

        SocketIOClient socketIO = SocketIOClient.getInstance();

        if(!socketIO.isConnected())
            socketIO.connect();

    }

    public void onProfileClick(View view){
        Log.d(LOG_TAG, "User Profile onClick Received!");
        // TODO : set the colors for the text.
        new MaterialDialog.Builder(this)
                .content(R.string.change_profile_picture)
                .positiveText(R.string.change)
                .negativeText(R.string.cancel)
                .theme(Theme.LIGHT)
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
        new Thread(logoutRunnable).start();
    }

    public void displayInvite(int sessionID, String inviteMessage){
        // TODO : see if we need to run this on the UI Thread
        new MaterialDialog.Builder(this)
                .content(inviteMessage)
                .positiveText(R.string.join)
                .negativeText(R.string.cancel)
                .theme(Theme.LIGHT)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        joinSession(sessionID);
                    }
                })
                .show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            Uri chosenImageUri = data.getData();

            Log.d(LOG_TAG, chosenImageUri.toString());

            try {

                // from : http://stackoverflow.com/questions/3879992/get-bitmap-from-an-uri-android
                InputStream input = this.getContentResolver().openInputStream(chosenImageUri);

                BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                onlyBoundsOptions.inJustDecodeBounds = true;
                onlyBoundsOptions.inDither=true;//optional
                onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
                BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
                input.close();

                int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

                double ratio = (originalSize > 200) ? (originalSize / 200) : 1.0;

                BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
                bitmapOptions.inDither=true;//optional
                bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
                input = this.getContentResolver().openInputStream(chosenImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
                input.close();


                Matrix matrix = new Matrix();

                // TODO : see if we are rotating this correctly.
                matrix.postRotate(ImageUtilities.getImageRotation(getApplicationContext() , chosenImageUri));

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);

                userProfileImageView.setImageBitmap(bitmap);
                getUploadThread(bitmap).start();
            } catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "Failed to load image due to IOException! Profile loading failed");
            } catch(OutOfMemoryError e){
                e.printStackTrace();
                Log.d(LOG_TAG , "OutOfMemoryError for profile picture!");
            }


        }
    }

    private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }

    public void addFriendClick(View v){
        addFriendDialog();
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
            new Thread(logoutRunnable).start();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Runnable logoutRunnable = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            CurrentUser.logOut();
            runOnUiThread(backToLoginActivityRunnable);
        }
    };

    private Runnable backToLoginActivityRunnable = new Runnable() {
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

            while(user == null || user.getName() == null || user.getUsername() == null){
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
            Picasso.with(getApplicationContext())
                    .load(user.getProfileURL())
                    .into((ImageView) findViewById(R.id.user_image));

            Log.d(LOG_TAG , "Current User done loading profile picture, assigning to ImageView");
            TextView name = (TextView) findViewById(R.id.current_user_name);
            name.setText(user.getName());

            TextView username = (TextView) findViewById(R.id.current_user_username);
            username.setText("@" + user.getUsername());
        }
    }

    /**
     * The method that will be called if the user is in a session, but it was not when this
     * activity was
     */
    public void sessionLoaded(){
        Intent intent = new Intent(getApplicationContext() , MainSessionActivity.class);
        startActivity(intent);
    }

    /**
     * The method called on the Floating Action Button click that will go to the
     * MainSessionActivity and create a session if the user is not currently in one.
     * @param view
     */
    public void onFABClick(View view){
        UnisongSession.notifyWhenLoaded(null);
        UnisongSession session = UnisongSession.getCurrentSession();
        if(session != null){
            // TODO : if we're in a session, move to the Session screen
        } else {
            // TODO : if not, then create a session

            session = new UnisongSession();

            CurrentUser.getInstance().setSession(session);
            UnisongSession.setCurrentSession(session);

        }

        Intent intent = new Intent(getApplicationContext() , MainSessionActivity.class);
        startActivity(intent);
    }

    public void joinSession(int ID ){
        UnisongSession session = SessionUtils.getSessionByID(ID);

        CurrentUser.getInstance().setSession(session);
        UnisongSession.setCurrentSession(session);

        Intent intent = new Intent(getApplicationContext() , MainSessionActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if(networkIntent != null)
            stopService(networkIntent);

        if(mediaIntent != null)
            stopService(mediaIntent);

        networkIntent = null;
        mediaIntent = null;
    }

    /**
     * The method that will be called when a friend row is clicked
     * If the friend is in a session we will join it, otherwise we will do nothing
     * @param view
     */
    public void onFriendClick(View view){
        try {

            UUID uuid = (UUID) view.getTag();

            Log.d(LOG_TAG , view.getTag().toString());
            User user = UserUtils.getUser(uuid);
            user.update();
            tempUserCheck = user;
            handler.removeCallbacks(mCheckUserSession);
            handler.postDelayed(mCheckUserSession, 200);

            // TODO : add dialog/popup for confirmation?

            /*
            if(user.getSession() != null){
                Log.d(LOG_TAG , "Selected user has a session! Joining");
                UnisongSession session = user.getSession();

                User currentUser = CurrentUser.getInstance();
                currentUser.setSession(session);

                SocketIOClient client = SocketIOClient.getInstance();

                client.joinSession(session.getSessionID());

                UnisongSession.setCurrentSession(session);

                Intent intent = new Intent(getApplicationContext() , MainSessionActivity.class);
                startActivity(intent);
            } else {
                Log.d(LOG_TAG , "User does not have a session, therefore we cannot join");
                return;
            }*/
        }catch (ClassCastException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "View tag was cast incorrectly!");
        } catch (NullPointerException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "Something was null in onFriendClick() !");
        }
    }

    private User tempUserCheck;
    private Runnable mCheckUserSession = () ->{
        if(tempUserCheck != null){
            if(tempUserCheck.getSession() != null){
                Log.d(LOG_TAG , "Selected user has a session! Joining");
                UnisongSession session = tempUserCheck.getSession();

                User currentUser = CurrentUser.getInstance();
                currentUser.setSession(session);

                SocketIOClient client = SocketIOClient.getInstance();

                client.joinSession(session.getSessionID());

                // Remember to nullify sessionToNotify
                UnisongSession.notifyWhenLoaded(null);
                UnisongSession.setCurrentSession(session);
                session.getMembers().add(currentUser);

                Intent intent = new Intent(getApplicationContext() , MainSessionActivity.class);
                startActivity(intent);
            }
        }

        tempUserCheck = null;
    };


    public static class IncomingHandler extends Handler{

        private UnisongActivity mActivity;
        public IncomingHandler(UnisongActivity activity){
            super();
            mActivity = activity;
        }


        @Override
        public void handleMessage(Message message) {

            switch (message.what){
                case INVITE:

                    try {
                        JSONObject object = (JSONObject) message.obj;
                        int sessionID = object.getInt("sessionID");
                        String inviteMessage = object.getString("message");
                        mActivity.displayInvite(sessionID , inviteMessage);

                    } catch(ClassCastException e){
                        e.printStackTrace();
                        Log.d(LOG_TAG , "Class cast incorrectly for invite!");
                    } catch (JSONException e){
                        e.printStackTrace();
                        Log.d(LOG_TAG , "JSONObject not in expected format for invite message!");
                    }
                    break;

            }
        }
    }

    @Override
    public void onBackPressed() {
    }

    public void addFriendDialog(){
        String[] options = getResources().getStringArray(R.array.add_friend_options);

        if(CurrentUser.getInstance().isFacebookUser()) {
            List<String> optionsArray = new ArrayList<String>(Arrays.asList(options));
            optionsArray.add("From Facebook");
            options = optionsArray.toArray(options);
        }

        new MaterialDialog.Builder(this)
                .title(R.string.add_friend_label)
                .items(options)
                .theme(Theme.LIGHT)
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        switch(which){
                            case 0:
                                Intent intent = new Intent(getApplicationContext(), AddFriendByUsernameActivity.class);
                                startActivity(intent);
                                break;

                            case 1:
                                intent = new Intent(getApplicationContext(), AddFriendsFromContactsActivity.class);
                                startActivity(intent);
                                break;

                            case 2:
                                intent = new Intent(getApplicationContext(), AddFriendsFromFacebookActivity.class);
                                startActivity(intent);
                                break;
                        }
                        // ??
                        return true;
                    }
                })
                .positiveText(R.string.choose)
                .show();
    }
}
