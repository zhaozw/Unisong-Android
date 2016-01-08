package io.unisong.android.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

import io.unisong.android.R;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.user.FriendsList;
import io.unisong.android.network.user.User;

/**
 * Created by Ethan on 9/22/2015.
 */
public class AddFriendByUsernameActivity extends AppCompatActivity{

    private final static String LOG_TAG = AddFriendByUsernameActivity.class.getSimpleName();
    private EditText mUsernameField;
    private String mUsername;
    private HttpClient mClient;


    @Override
    public void onCreate(Bundle savedInstanceState){
        // TODO : add lifecycle persistence and whatnot
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend_by_username);
        mUsernameField = (EditText) findViewById(R.id.add_friend_by_username_edit_text);
        mClient = HttpClient.getInstance();
    }

    public void addFriend(View view){
        mUsername = mUsernameField.getText().toString();
        new AddFriendTask().execute(mUsername);
    }

    private class AddFriendTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... strings) {
            String username = mUsername;

            Response response;
            try {
                 response = mClient.syncGet(NetworkUtilities.HTTP_URL + "/user/get-by-username/" + username);
            } catch (IOException e){
                e.printStackTrace();
                return "Failure";
            }

            String userID;
            if(response.code() == 404){
                return "Failure";
            } else if(response.code() == 200){
                try{
                    String body = response.body().string();
                    Log.d(LOG_TAG , body);
                    JSONObject object = new JSONObject(body);
                    userID = object.getString("userID");
                    Log.d(LOG_TAG, "User exists, userID is : " + userID);
                } catch (JSONException e){
                    e.printStackTrace();
                    return "Failure";
                } catch (IOException e){
                    e.printStackTrace();
                    return "Body parsing failed";
                }
            } else {
                return "Failure";
            }

            try{
                JSONObject object = new JSONObject();
                object.put("friendID" , userID);
                response = mClient.syncPost(NetworkUtilities.HTTP_URL + "/user/friends" , object);
            } catch (IOException e){
                e.printStackTrace();
                return "Failure";
            } catch (JSONException e){
                e.printStackTrace();
                return "JSONException Failure";
            }

            if(response.code() == 200){
                FriendsList list = FriendsList.getInstance();
                list.addFriend(new User(UUID.fromString(userID)));
                return "Friend added!";
            }
            return "Failure";
        }

        @Override
        protected void onPostExecute(String result){
            Toast toast = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG);
            toast.show();
        }
    }

}
