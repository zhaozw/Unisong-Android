package io.unisong.android.activity.register;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.iangclifton.android.floatlabel.FloatLabel;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

import io.unisong.android.PrefUtils;
import io.unisong.android.R;
import io.unisong.android.activity.UnisongActivity;
import io.unisong.android.network.NetworkUtilities;
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
    private FloatLabel phoneNumber;
    private FloatLabel username;
    private String formattedPhoneNumber;

    private HttpClient client;
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number_input_fb);

        toolbar = (Toolbar) findViewById(R.id.session_bar);
        setSupportActionBar(toolbar);

        phoneNumber = (FloatLabel) findViewById(R.id.register_phone_number);
        username =    (FloatLabel) findViewById(R.id.register_username);

        phoneNumber.getEditText().addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        client = HttpClient.getInstance();
    }

    public void onRegister(View view){


        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        String phone = phoneNumber.getEditText().getText().toString();

        try{
            Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(phone, "US");
            formattedPhoneNumber = phoneUtil.format(phoneNumber , PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e){
            Toast invalidNumberToast = Toast.makeText(getBaseContext() , "Phone Number Invalid! US numbers only!" , Toast.LENGTH_LONG);
            invalidNumberToast.show();
            // currently only support US numbers?
            e.printStackTrace();
            Log.d(LOG_TAG , "Parsing Phone number failed!");
            return;
        }

        HttpClient client = HttpClient.getInstance();
        client.get(NetworkUtilities.HTTP_URL + "/user/get-phone-confirmation-code/" + formattedPhoneNumber, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {

            }
        });

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
        String username =  this.username.getEditText().getText().toString();
        String phonenumber = formattedPhoneNumber;
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
