package io.unisong.android.network.user;


import android.graphics.Bitmap;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.facebook.AccessToken;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.session.SessionUtils;
import io.unisong.android.network.session.UnisongSession;

/**
 * The class for containing user information.
 * Created by Ethan on 8/9/2015.
 */
public class User implements Serializable {

    private transient final static String LOG_TAG = User.class.getSimpleName();

    private boolean isFacebookUser;
    private transient HttpClient client;
    private String facebookID, username, phoneNumber, name, password;
    private UUID uuid;
    private UnisongSession session;

    // The boolean tag to tell us if we failed to get the profile picture
    private boolean mRetrieveProfilePictureFailed;

    // The boolean that tells us if we're done retrieving the profile picture
    private boolean mHasProfilePicture;

    // The access token for facebook users.

    /**
     * Instantiate
     */
    private User(){
        UserUtils.addUser(this);
        client = HttpClient.getInstance();
        mHasProfilePicture = false;
        mRetrieveProfilePictureFailed = false;
    }


    public User(String username, String password){
        this();
        this.username = username;
        this.password = password;
        getUserInfoThread().start();

        // TODO : load rest of info from server and save.
    }

    public User(UUID uuid){
        this();
        this.uuid = uuid;
        getUserInfoThread().start();
    }


    /**
     * Creates an user from a facebook access token
     * This will attempt to read from the file {facebookID}.user in our app directory
     * and deserialize everything from there.
     * @param accessToken
     */
    public User(AccessToken accessToken){
        this();
        facebookID = accessToken.getUserId();
        isFacebookUser = true;
        Log.d(LOG_TAG, "Initializing facebook user.");
        getUserInfoThread().start();
    }

    private Thread getUserInfoThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                getUserInfo();
                checkSessionStatus();
            }
        });
    }

    public void checkSessionStatus(){
        try {

            while(uuid == null){
                synchronized (this){
                    try {
                        this.wait(10);
                    } catch (InterruptedException e){

                    }
                }
            }
            Response response = client.syncGet(NetworkUtilities.HTTP_URL + "/user/" + uuid.toString() + "/session/");

            Log.d(LOG_TAG , "response");

            if(response.code() == 200){
                int id = Integer.parseInt(response.body().string());

                Log.d(LOG_TAG , "Session created");
                session = SessionUtils.getSessionByID(id);

                User currentUser = CurrentUser.getInstance();
                if(currentUser != null && this.equals(currentUser)){
                    UnisongSession.setCurrentSession(session);
                }
            } else if(response.code() == 404){
                session = null;
            }

        } catch (IOException e){
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "NullPointerException in checkSessionStatus, perhaps HttpClient was destroyed?");
        }
    }

    public void setPassword(String password){
        this.password = password;
    }

    public String getPassword(){
        return password;
    }

    public String getUsername(){
        return username;
    }

    public UUID getUUID(){
        return uuid;
    }

    public String getName(){
        return name;
    }

    public String getPhoneNumber(){
        return phoneNumber;
    }

    public void setSession(UnisongSession session){
        this.session = session;
    }

    public UnisongSession getSession() {
        return session;
    }


    public boolean equals(User user){
        if(user.getUsername() != null && username != null)
            if(user.getUsername().equals(username))
                return true;

        if(user.getUUID() != null && uuid != null)
            if(user.getUUID().equals(uuid))
                return true;

        if(user.getFacebookID() != null && facebookID != null)
            if(user.getFacebookID().equals(facebookID))
                return true;

        return false;
    }

    public String getProfileURL(){
        if(isFacebookUser){
            return "http://graph.facebook.com/" + facebookID + "/picture?type=large";
        } else {
            return NetworkUtilities.HTTP_URL + "/user/" + uuid.toString() + "/profile/picture/";
        }
    }


    public boolean isFacebookUser(){
        if(facebookID != null ){
            return true;
        } else {
            return false;
        }
    }


    /**
     * This method retrieves user info from the server and assigns it to relevant fields.
     * TODO : figure out how to properly mix this with cached data.
     */
    private void getUserInfo(){

        Callback profileCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
                parseResponse(response);
            }
        };

        try {
            if (username != null && !username.equals("")) {
                client.get(NetworkUtilities.HTTP_URL + "/user/get-by-username/" + username, profileCallback);
            } else if (uuid != null) {
                client.get(NetworkUtilities.HTTP_URL + "/user/" + uuid, profileCallback);
            } else if (facebookID != null) {
                client.get(NetworkUtilities.HTTP_URL + "/user/get-by-facebookID/" + facebookID, profileCallback);
            } else {
                return;
            }
        } catch (IOException e){
            e.printStackTrace();
            return;
        }
    }

    private void parseResponse(Response response){
        JSONObject object;
        try {
            String body = response.body().string();
            Log.d(LOG_TAG , body);
            object = new JSONObject(body);
            uuid = UUID.fromString(object.getString("userID"));
            username = object.getString("username");
            phoneNumber = object.getString("phone_number");
            name = object.getString("name");
            if(object.has("facebookID")) {
                long facebookID = object.getLong("facebookID");
                if(facebookID != 0){
                    isFacebookUser = true;
                    this.facebookID =  facebookID + "";
                } else {
                    isFacebookUser = false;
                }
            } else {
                isFacebookUser = false;
            }
        } catch (IOException e){
            e.printStackTrace();
            // TODO : retry
            return;
        } catch (JSONException e){
            e.printStackTrace();
            // TODO : investigate when this could happen
            return;
        }
    }

    public void uploadProfilePicture(Bitmap bitmap){
        // TODO : don't use bitmaps?
        Log.d(LOG_TAG, "Bitmap size: " + bitmap.getByteCount() + " bytes.");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        } catch (NullPointerException e){
            e.printStackTrace();
            return;
        }

        byte[] byteArray = byteArrayOutputStream .toByteArray();
        Log.d(LOG_TAG , "Byte array size: " + byteArray.length + " bytes.");
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        Log.d(LOG_TAG , "Encoded getBytes() size : " + encoded.getBytes().length + " bytes.");

        uploadPicture(encoded);
    }


    private void uploadPicture(String encoded){
        JSONObject object = new JSONObject();
        try {
            object.put("data", encoded);
        } catch (JSONException e){
            e.printStackTrace();
            return;
        }

        Callback uploadCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                // TODO : implement?
            }

            @Override
            public void onResponse(Response response) throws IOException {
                // TODO : implement?
            }
        };

        try {
            client.post(NetworkUtilities.HTTP_URL + "/user/profile/picture", object, uploadCallback);
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    public boolean hasProfilePicture(){
        return mHasProfilePicture;
    }

    // TODO : make async
    private Thread getSessionStatusThread(){
        return new Thread(this::checkSessionStatus);
    }

    public boolean profileRetrievalFailed(){
        return mRetrieveProfilePictureFailed;
    }

    public String getFacebookID(){
        return facebookID;
    }

    public void update(){
        getSessionStatusThread().start();
    }

    public String toString(){
        return "User : { username: " + username + "}";
    }
}
