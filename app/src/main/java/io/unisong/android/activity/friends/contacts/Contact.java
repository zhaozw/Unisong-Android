package io.unisong.android.activity.friends.contacts;

import android.support.annotation.Nullable;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.user.User;
import io.unisong.android.network.user.UserUtils;

/**
 * This class is loaded by the ContactsLoader class, and contains information about a contact
 * within a User's phone. This can be used to add friends from matching their Contact list,
 * or to invite a friend to use this application.
 * Created by Ethan on 8/9/2015.
 */
public class Contact {

    private static final String LOG_TAG = Contact.class.getSimpleName();
    private String name, phoneNumber;
    private boolean userExists = false;
    // The user matching the information in this contact, if it exists, null otherwise
    @Nullable
    private User user;


    public Contact(String name, String phoneNumber){
        this.name = name;
        this.phoneNumber = phoneNumber;
        HttpClient client = HttpClient.getInstance();
        client.get(NetworkUtilities.HTTP_URL + "/user/get-by-phonenumber/" + phoneNumber, checkUserExists);
    }

    public String getName(){
        return name;
    }

    /**
     * Returns the phone number of this contact in E164 format
     * @return phoneNumber - the string in which the phone number is encoded
     */
    public String getPhoneNumber(){
        return phoneNumber;
    }

    private Callback checkUserExists = new Callback() {
        @Override
        public void onFailure(Request request, IOException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "Checking for user existence failed!");
        }

        @Override
        public void onResponse(Response response) throws IOException {
            if(response.code() == 200){
                try {
                    Log.d(LOG_TAG , "User exists!");
                    String body = response.body().toString();
                    JSONObject userJSON = new JSONObject(body);
                    try{

                        String phoneNumber = userJSON.getString("phonenumber");

                        user = UserUtils.getUserByPhone(phoneNumber);

                        if(user == null)
                            user = new User(userJSON);

                    } catch (JSONException e){
                        e.printStackTrace();
                        Log.d(LOG_TAG , "JSONException thrown in Contact loading user!");
                    }

                } catch (JSONException e){
                    e.printStackTrace();
                    Log.d(LOG_TAG , "Parsing response failed!");
                }
            }
        }
    };

    @Nullable
    public UUID getUserUUID(){
        if(user != null)
            return user.getUUID();

        return null;
    }

    @Nullable
    public User getUser(){
        return user;
    }

    public boolean userExists(){
        return user != null;
    }
}
