package io.unisong.android;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ImageView;

import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;

/**
 * Created by ezturner on 9/17/2015.
 */
public class ProfileActivity extends ActionBarActivity {


    private User mUser;
    private ImageView mProfilePictureView;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mProfilePictureView = (ImageView) findViewById(R.id.profile_picture_view);

        mUser = CurrentUser.getInstance().getUser();

        if(mUser.isFacebookUser()){

        }

    }
}
