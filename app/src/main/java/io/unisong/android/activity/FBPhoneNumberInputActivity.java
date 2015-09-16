package io.unisong.android.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import io.unisong.android.R;

/**
 * This activity is presented after a user logs in with facebook for the first time.
 * It will attempt to get a username and phone number from the user, and then verify the number with an
 * SMS message.
 *
 * Created by ezturner on 9/16/2015.
 */
public class FBPhoneNumberInputActivity extends ActionBarActivity{

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number_input_fb);
    }
}
