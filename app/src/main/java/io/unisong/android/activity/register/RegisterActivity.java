package io.unisong.android.activity.register;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.iangclifton.android.floatlabel.FloatLabel;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.unisong.android.PrefUtils;
import io.unisong.android.R;
import io.unisong.android.activity.UnisongActivity;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;

/**
 * This activity handles user registration. If a user desires to register with a Username
 * and password, then they can, or they can press the Facebook Register button and bypass
 * that that way
 * Created by ezturner on 7/15/2015.
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String LOG_TAG = RegisterActivity.class.getSimpleName();

    private Toolbar toolbar;

    private String formattedPhoneNumber;
    private FloatLabel username, password, passwordVerify, phoneNumber;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        username = (FloatLabel) findViewById(R.id.registerUsername);
        password = (FloatLabel) findViewById(R.id.registerPassword);
        passwordVerify = (FloatLabel) findViewById(R.id.registerPasswordVerify);
        phoneNumber = (FloatLabel) findViewById(R.id.register_phone_number);

        toolbar = (Toolbar) findViewById(R.id.login_bar);

        phoneNumber.getEditText().addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    public void register(View view){

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

        // first, validate the fields.
        String password = this.password.getEditText().getText().toString();
        String passwordConfirm = passwordVerify.getEditText().getText().toString();

        if(!password.equals(passwordConfirm)){
            Toast badPasswords = Toast.makeText(getBaseContext() , "Passwords do not match!" , Toast.LENGTH_LONG);
            badPasswords.show();
            return;
        }

        Callback verifyCodeCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
                Log.d(LOG_TAG , "Retrieving verification code for phone # failed");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if(response.code() == 200){
                    getRegisterThread().start();

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
                registerRequest();
            }
        });
    }

    private void registerRequest() {

        Log.d(LOG_TAG , "Starting register request.");
        HttpClient client = HttpClient.getInstance();

        String username = this.username.getEditText().getText().toString();
        String password = this.password.getEditText().getText().toString();
        String phonenumber = formattedPhoneNumber;

        if(!PhoneNumberUtils.isGlobalPhoneNumber(phonenumber)){
            //TODO: tell the user that this is not a valid phone number
        }
        String URL = NetworkUtilities.EC2_INSTANCE + "/register";
        JSONObject json = new JSONObject();

        try {
            json.put("username", username);
            json.put("password", password);
            json.put("phone_number", phonenumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String result = "Error!";

        Callback callback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                runOnUiThread( () -> {
                    Toast toast = Toast.makeText(getBaseContext() , "Registration failed due to error!" , Toast.LENGTH_LONG );
                    toast.show();
                });

            }

            @Override
            public void onResponse(Response response) throws IOException {
                if(response.code() == 200){
                    registerSuccess(username , password);
                }
            }
        };


        client.post(URL, json, callback);

    }

    private void registerSuccess(String username , String password){
        //TODO : save account credentials with AccountManager/shared prefs/something
        PrefUtils.saveToPrefs(getApplicationContext() , PrefUtils.PREFS_LOGIN_USERNAME_KEY , username);
        PrefUtils.saveToPrefs(getApplicationContext() , PrefUtils.PREFS_LOGIN_PASSWORD_KEY, password);
        PrefUtils.saveToPrefs(getApplicationContext() , PrefUtils.PREFS_ACCOUNT_TYPE_KEY , "unisong");
        Intent intent = new Intent(getApplicationContext() , UnisongActivity.class);
        startActivity(intent);
        finish();
    }
}
