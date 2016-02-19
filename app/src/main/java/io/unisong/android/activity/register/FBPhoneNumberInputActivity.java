package io.unisong.android.activity.register;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.facebook.AccessToken;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.iangclifton.android.floatlabel.FloatLabel;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.unisong.android.PrefUtils;
import io.unisong.android.R;
import io.unisong.android.activity.UnisongActivity;
import io.unisong.android.activity.friends.contacts.AddFriendsFromContactsActivity;
import io.unisong.android.activity.friends.facebook.AddFriendsFromFacebookActivity;
import io.unisong.android.activity.friends.username.AddFriendByUsernameActivity;
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

        Callback verifyCodeCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
                if(response.code() == 200) {
                    getRegisterThread().start();
                } else {
                    runOnUiThread(() -> {
                        String message = getResources().getString(R.string.wrong_code);
                        Toast toast = Toast.makeText(getApplicationContext() , message , Toast.LENGTH_LONG);
                        toast.show();
                    });
                }
            }
        };


        new MaterialDialog.Builder(this)
                .title(R.string.verification_code)
                .content(R.string.verification_code_text)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .theme(Theme.LIGHT)
                .input(R.string.verification_code, R.string.empty, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        // Do something
                        HttpClient.getInstance().post(NetworkUtilities.HTTP_URL +
                                "/user/verify-phone-confirmation-code/" + formattedPhoneNumber
                                + "/code/" + input.toString() , new JSONObject(), verifyCodeCallback );

                    }
                }).show();
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
        PrefUtils.saveToPrefs(this, PrefUtils.PREFS_HAS_LOGGED_IN_KEY , "yes");

        client.loginFacebook(AccessToken.getCurrentAccessToken(), username, phonenumber);
        FacebookAccessToken.saveFacebookAccessToken(getApplicationContext());


        Intent intent = new Intent(getApplicationContext(), UnisongActivity.class);
        startActivity(intent);
        finish();
        /*
        String fromContacts = getResources().getString(R.string.from_contacts);
        String fromFacebook = getResources().getString(R.string.from_facebook);
        String[] options = new String[] {fromContacts , fromFacebook};

        runOnUiThread(() -> {
            new MaterialDialog.Builder(this)
                    .title(R.string.add_friend_label)
                    .items(options)
                    .theme(Theme.LIGHT)
                    .negativeText(R.string.no_thanks)
                    .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                            switch (which) {
                                case 0:
                                    Intent intent = new Intent(getApplicationContext(), AddFriendsFromContactsActivity.class);
                                    intent.putExtra("fromRegister", true);
                                    startActivity(intent);
                                    finish();
                                    break;

                                case 1:
                                    intent = new Intent(getApplicationContext(), AddFriendsFromFacebookActivity.class);
                                    intent.putExtra("fromRegister", true);
                                    startActivity(intent);
                                    finish();
                                    break;
                            }
                            return true;
                        }
                    })
                    .onNegative((MaterialDialog materialDialog, DialogAction dialogAction) -> {
                        Intent intent = new Intent(getApplicationContext(), UnisongActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .positiveText(R.string.choose)
                    .show();
        });*/
    }

}
