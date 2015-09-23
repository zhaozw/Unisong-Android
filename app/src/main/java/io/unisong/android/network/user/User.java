package io.unisong.android.network.user;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import com.facebook.AccessToken;

import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * The class for containing user information.
 * Created by Ethan on 8/9/2015.
 */
public class User implements Serializable {

    private transient final static String LOG_TAG = User.class.getSimpleName();

    private transient AccessToken mFBAccessToken;
    private String mFacebookID;
    private transient HttpClient mClient;
    private String mUsername;
    private String mPhoneNumber;
    private UUID mUUID;
    private String mProfilePictureCachePath;
    private String mProfilePictureS3Key;
    private String mName;
    private byte[] mProfilePicture;
    private Context mContext;

    // The boolean tag to tell us if we failed to get the profile picture
    private boolean mRetrieveProfilePictureFailed;

    // The boolean that tells us if we're done retrieving the profile picture
    private boolean mHasProfilePicture;

    // The access token for facebook users.

    /**
     * Instantiate
     */
    private User(Context context){
        mClient = HttpClient.getInstance();
        mContext = context;
        mHasProfilePicture = false;
        mRetrieveProfilePictureFailed = false;
    }


    public User(Context context , String username){
        this(context);
        mUsername = username;
        getUserInfoThread().start();

        // TODO : load rest of info from server and save.
    }

    public User(Context context , UUID uuid){
        this(context);
        mUUID = uuid;
        getUserInfoThread().start();
    }


    /**
     * Creates an user from a facebook access token
     * This will attempt to read from the file {facebookID}.user in our app directory
     * and deserialize everything from there.
     * @param accessToken
     */
    public User(Context context , AccessToken accessToken){
        this(context);
        mFBAccessToken = accessToken;
        mFacebookID = accessToken.getUserId();
        Log.d(LOG_TAG , "Initializing facebook user.");
        getUserInfoThread().start();
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


    public boolean equals(User user){
        // todo : check for null?
        if(user.getUsername().equals(mUsername) || user.getUUID().equals(mUUID)){
            return true;
        }
        return false;
    }

    public Bitmap getProfilePicture(){
        if(mProfilePicture != null) {
            return BitmapFactory.decodeByteArray(mProfilePicture, 0, mProfilePicture.length);
        }
        return null;
    }

    private void getFacebookProfilePicture(){
        HttpUrl imageUrl = HttpUrl.parse("http://graph.facebook.com/" + mFBAccessToken.getUserId() + "/picture?type=large");

        // If we don't parse the url correctly return null
        // TODO : handle error?

        OkUrlFactory factory = new OkUrlFactory(mClient.getClient());

//        Response response = mClient.get(imageUrl)


        InputStream inputStream;

        try{
            inputStream = factory.open(imageUrl.url()).getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, read);
            }

            mProfilePicture = baos.toByteArray();
            mHasProfilePicture = true;
            cacheProfilePicture();
        } catch (IOException e){
            e.printStackTrace();
            mRetrieveProfilePictureFailed = true;
        }

    }

    /**
     * Returns true if getting the cached picture was successful.
     * Will also set the
     * @return
     */
    private boolean getCachedPicture(){
        File file = new File(mContext.getCacheDir().getAbsolutePath() + "/" + getProfilePictureCachePath());

        if(!file.exists()){
            return false;
        } else {
            Log.d(LOG_TAG , "Loading cached unisong picture.");
            InputStream in;
            try {
                in = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            try {
                mProfilePicture = new byte[in.available()];
                in.read(mProfilePicture);
                mHasProfilePicture = true;
            } catch (IOException e){
                e.printStackTrace();
                return false;
            }

            return true;
        }
    }

    /**
     * Returns true if getting the cached picture was successful.
     * Will also set the
     * @return
     */
    private boolean getCachedFacebookPicture(){
        File file = new File(mContext.getCacheDir().getAbsolutePath() + "/" + getProfilePictureCachePath());

        if(!file.exists() || System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000){
            return false;
        } else {
            Log.d(LOG_TAG, "Loading cached facebook picture.");
            InputStream in;
            try {
                in = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            try {
                mProfilePicture = new byte[in.available()];
                in.read(mProfilePicture);
                mHasProfilePicture = true;
            } catch (IOException e){
                e.printStackTrace();
                return false;
            }

            return true;
        }
    }

    private void cacheProfilePicture(){
        File file = new File(mContext.getCacheDir().getAbsolutePath() + "/" + getProfilePictureCachePath());

        if(file.exists()){
            file.delete();
        }
        OutputStream out;
        try {
            out = new FileOutputStream(file);
        } catch (FileNotFoundException e){
            e.printStackTrace();
            return ;
        }

        try {
            out.write(mProfilePicture);
            out.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the Unisong profile picture for this user.
     */
    private void getUnisongProfilePicture(){
        try {
            // todo : uncomment after test
            //Response response = mClient.get(NetworkUtilities.HTTP_URL + "/user/ " + mUUID.toString() + " /profile/picture/");
            Response response = mClient.get(NetworkUtilities.HTTP_URL + "/user/profile/picture/");
            String data;

            try{
                String body = response.body().string();
                JSONObject object = new JSONObject(body);
                data = object.getString("data");
            } catch (JSONException e){
                e.printStackTrace();
                mRetrieveProfilePictureFailed = true;
                return;
            }

            Log.d(LOG_TAG, "Loaded!");

            mProfilePicture = Base64.decode(data, Base64.DEFAULT);
            mHasProfilePicture = true;
            cacheProfilePicture();

        } catch (IOException e){
            mRetrieveProfilePictureFailed = true;
            e.printStackTrace();
        }
    }

    private String getProfilePictureCachePath(){
        return mUsername + ".profile_picture.jpeg";
    }

    private void loadProfilePicture(){



        if(mFacebookID != null){
            File file = new File(mContext.getCacheDir().getAbsolutePath() + "/" + getProfilePictureCachePath());

            if(file.exists() && System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000){
                file.delete();
            } else {
                if(getCachedPicture()){
                    return;
                }
            }

            getFacebookProfilePicture();
            return;
        }

        if(!getCachedPicture()){
            getUnisongProfilePicture();
        }
    }

    public boolean isFacebookUser(){
        if(mFacebookID != null ){
            return true;
        } else {
            return false;
        }
    }


    private Thread getUserInfoThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                getUserInfo();
                loadProfilePicture();
            }
        });
    }

    /**
     * This method retrieves user info from the server and assigns it to relevant fields.
     * TODO : figure out how to properly mix this with cached data.
     */
    private void getUserInfo(){
        Response response;
        try {
            if (mUsername != null && !mUsername.equals("")) {
                response = mClient.get(NetworkUtilities.HTTP_URL + "/user/get-by-username/" + mUsername);
            } else if (mUUID != null) {
                response = mClient.get(NetworkUtilities.HTTP_URL + "/user/" + mUUID);
            } else if (mFacebookID != null) {
                response = mClient.get(NetworkUtilities.HTTP_URL + "/user/get-by-facebookID/" + mFacebookID);
            } else {
                return;
            }
        } catch (IOException e){
            e.printStackTrace();
            return;
        }
        JSONObject object;
        try {
            String body = response.body().string();
            Log.d(LOG_TAG , body);
            object = new JSONObject(body);
            mUUID = UUID.fromString(object.getString("userID"));
            mUsername = object.getString("username");
            mPhoneNumber = object.getString("phone_number");
            mName = object.getString("name");
            mFacebookID = object.getLong("facebookID") + "";
            if(mFacebookID.equals("0")){
                mFacebookID = null;
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

        try {
            mClient.post(NetworkUtilities.HTTP_URL + "/user/profile/picture", object);
        } catch (IOException e){
            e.printStackTrace();
        }

        System.gc();
    }

    public boolean doesNotHaveProfilePicture(){
        if(!mRetrieveProfilePictureFailed) {
            return !mHasProfilePicture;
        } else {
            return true;
        }
    }
}
