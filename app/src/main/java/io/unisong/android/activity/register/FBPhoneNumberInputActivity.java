package io.unisong.android.activity.register;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.facebook.AccessToken;

import io.unisong.android.PrefUtils;
import io.unisong.android.R;
import io.unisong.android.activity.UnisongActivity;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.user.FacebookAccessToken;

/**
 * This activity is presented after a user logs in with facebook for the first time.
 * It will attempt to get a username and phone number from the user, and then verify the number with an
 * SMS message.
 *
 * Created by ezturner on 9/16/2015.
 */
public class FBPhoneNumberInputActivity extends ActionBarActivity{

    private static final String LOG_TAG = FBPhoneNumberInputActivity.class.getSimpleName();
    private EditText phoneNumber;
    private EditText username;

    private HttpClient client;
    private String email;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number_input_fb);

        phoneNumber = (EditText) findViewById(R.id.facebook_phone_number_input);
        username = (EditText) findViewById(R.id.facebook_username_input);

        phoneNumber.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        email = new String(getIntent().getCharArrayExtra("email"));

        client = HttpClient.getInstance();
    }

    public void onRegister(View view){
        Log.d(LOG_TAG, "onRegister called, starting register thread.");
        getRegisterThread().start();
    }

    private Thread getRegisterThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                register();
            }
        });
    }

    private void register(){
        // TODO : add error handling?
        // TODO : input validation and checking against server.
        String username = this.username.getText().toString();
        String phonenumber = phoneNumber.getText().toString();
        PrefUtils.saveToPrefs(getApplicationContext() , PrefUtils.PREFS_LOGIN_USERNAME_KEY, username);
        PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "facebook");

        phonenumber = phonenumber.replace("(" , "");
        phonenumber = phonenumber.replace(")" , "");
        phonenumber = phonenumber.replace("-" , "");
        phonenumber = phonenumber.replace(" " , "");

        client.loginFacebook(AccessToken.getCurrentAccessToken(), username, phonenumber);
        FacebookAccessToken.saveFacebookAccessToken(getApplicationContext());

        Intent intent = new Intent(getApplicationContext() , UnisongActivity.class);
        startActivity(intent);
        finish();
    }

}
