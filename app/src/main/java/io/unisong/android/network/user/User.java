package io.unisong.android.network.user;


import android.content.Context;
import android.graphics.Bitmap;
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

    private boolean mIsFacebookUser;
    private String mFacebookID;
    private transient HttpClient mClient;
    private String mUsername;
    private String mPhoneNumber;
    private UUID mUUID;
    private String mName;
    private Context mContext;
    private String mPassword;
    private UnisongSession mSession;

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
        mClient = HttpClient.getInstance();
        mHasProfilePicture = false;
        mRetrieveProfilePictureFailed = false;
        getUserInfoThread().start();
    }


    public User(String username, String password){
        this();
        mUsername = username;
        mPassword = password;

        // TODO : load rest of info from server and save.
    }

    public User(UUID uuid){
        this();
        mUUID = uuid;
    }


    /**
     * Creates an user from a facebook access token
     * This will attempt to read from the file {facebookID}.user in our app directory
     * and deserialize everything from there.
     * @param accessToken
     */
    public User(AccessToken accessToken){
        this();
        mFacebookID = accessToken.getUserId();
        mIsFacebookUser = true;
        Log.d(LOG_TAG, "Initializing facebook user.");
    }

    private Thread getUserInfoThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                getUserInfo();
                checkSessionStatus();
                //loadProfilePicture();
            }
        });
    }

    public void checkSessionStatus(){
        try {

            while(mUUID == null){
                synchronized (this){
                    try {
                        this.wait(10);
                    } catch (InterruptedException e){

                    }
                }
            }
            Response response = mClient.syncGet(NetworkUtilities.HTTP_URL + "/user/" + mUUID.toString() + "/session/");

            Log.d(LOG_TAG , "response");

            if(response.code() == 200){
                int id = Integer.parseInt(response.body().string());

                Log.d(LOG_TAG , "Session created");
                mSession = SessionUtils.getSessionByID(id);

                User currentUser = CurrentUser.getInstance();
                if(currentUser != null && this.equals(currentUser)){
                    UnisongSession.setCurrentSession(mSession);
                }
            } else if(response.code() == 404){
                mSession = null;
            }

        } catch (IOException e){
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "NullPointerException in checkSessionStatus, perhaps HttpClient was destroyed?");
        }
    }

    public void setPassword(String password){
        mPassword = password;
    }

    public String getPassword(){
        return mPassword;
    }

    public String getUsername(){
        return mUsername;
    }

    public UUID getUUID(){
        return mUUID;
    }

    public String getName(){
        return mName;
    }

    public String getPhoneNumber(){
        return mPhoneNumber;
    }

    public void setSession(UnisongSession session){
        mSession = session;
    }

    public UnisongSession getSession() {
        return mSession;
    }


    public boolean equals(User user){
        if(user.getUsername() != null && mUsername != null)
            if(user.getUsername().equals(mUsername))
                return true;

        if(user.getUUID() != null && mUUID != null)
            if(user.getUUID().equals(mUUID))
                return true;

        if(user.getFacebookID() != null && mFacebookID != null)
            if(user.getFacebookID().equals(mFacebookID))
                return true;

        return false;
    }

    public String getProfileURL(){
        if(mIsFacebookUser){
            return "http://graph.facebook.com/" + mFacebookID + "/picture?type=large";
        } else {
            return NetworkUtilities.HTTP_URL + "/user/" + mUUID.toString() + "/profile/picture/";
        }
    }


    public boolean isFacebookUser(){
        if(mFacebookID != null ){
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
            if (mUsername != null && !mUsername.equals("")) {
                mClient.get(NetworkUtilities.HTTP_URL + "/user/get-by-username/" + mUsername ,profileCallback);
            } else if (mUUID != null) {
                mClient.get(NetworkUtilities.HTTP_URL + "/user/" + mUUID , profileCallback);
            } else if (mFacebookID != null) {
                mClient.get(NetworkUtilities.HTTP_URL + "/user/get-by-facebookID/" + mFacebookID , profileCallback);
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
            mUUID = UUID.fromString(object.getString("userID"));
            mUsername = object.getString("username");
            mPhoneNumber = object.getString("phone_number");
            mName = object.getString("name");
            if(object.has("facebookID")) {
                long facebookID = object.getLong("facebookID");
                if(facebookID != 0){
                    mIsFacebookUser = true;
                    mFacebookID =  facebookID + "";
                } else {
                    mIsFacebookUser = false;
                }
            } else {
                mIsFacebookUser = false;
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

    public JSONObject toJSONObject() throws JSONException {
        JSONObject user = new JSONObject();
        return user;
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
            mClient.post(NetworkUtilities.HTTP_URL + "/user/profile/picture", object, uploadCallback);
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
        return mFacebookID;
    }

    public void update(){
        getSessionStatusThread().start();
    }

    public String toString(){
        return "User : { username: " + mUsername + "}";
    }
}
