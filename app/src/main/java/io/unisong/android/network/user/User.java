package io.unisong.android.network.user;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
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
    private Bitmap mProfilePicture;

    // The access token for facebook users.

    /**
     * Instantiate
     */
    private User(){
        mClient = HttpClient.getInstance();
    }


    public User(String username){
        this();
        mUsername = username;
        getUserInfoThread().start();

        // TODO : load rest of info from server and save.
    }

    public User(UUID uuid){
        this();
        mUUID = uuid;
        getUserInfoThread().start();
    }


    /**
     * Creates an user from a facebook access token
     * This will attempt to read from the file {facebookID}.user in our app directory
     * and deserialize everything from there.
     * @param accessToken
     */
    public User(AccessToken accessToken){
        mFBAccessToken = accessToken;
        mFacebookID = accessToken.getUserId();
        getUserInfoThread().start();
    }


    /**
     * Creates an User object from a set of bytes, ususally used by
     * @param data
     */
    public User(byte[] data){

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
        return mProfilePicture;
    }

    private class DownloadFilesTask extends AsyncTask<URL, Integer, Long> {

        @Override
        protected Long doInBackground(URL... urls) {
            return null;
        }
    }


    private ImageView mImageView;


    private void getFacebookProfilePicture(){
        HttpUrl imageUrl = HttpUrl.parse("http://graph.facebook.com/" + mFBAccessToken.getUserId() + "/picture?type=small");

        // If we don't parse the url correctly return null
        // TODO : handle error?

        OkUrlFactory factory = new OkUrlFactory(mClient.getClient());

//        Response response = mClient.get(imageUrl)

        Bitmap bitmap = null;
        try {
             bitmap = BitmapFactory.decodeStream(factory.open(imageUrl.url()).getInputStream());
        } catch (IOException e){
            e.printStackTrace();

        }

        mProfilePicture = bitmap;
    }

    /**
     * Returns true if getting the cached picture was successful.
     * Will also set the
     * @return
     */
    private boolean getCachedPicture(){
        if(mProfilePictureCachePath == null){
            return false;
        }
        File file = new File(mProfilePictureCachePath);

        InputStream in;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e){
            e.printStackTrace();
            return false;
        }

        Bitmap bitmap = BitmapFactory.decodeStream(in);
        mImageView.setImageBitmap(bitmap);

        return true;
    }

    private void cacheProfilePicture(){
        File file = new File(getProfilePictureCachePath());

        OutputStream out;
        try {
            out = new FileOutputStream(file);
        } catch (FileNotFoundException e){
            e.printStackTrace();
            return ;
        }
        if(mProfilePicture != null){
            mProfilePicture.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }

    /**
     * Retrieves the Unisong profile picture for this user.
     */
    private void getUnisongProfilePicture(){
        try {
            Response response = mClient.get(NetworkUtilities.HTTP_URL + "/user/ " + mUUID.toString() + " /profile/picture/");

            String data;

            try{
                JSONObject object = new JSONObject(response.body().string());
                data = object.getString("data");
            } catch (JSONException e){
                e.printStackTrace();
                return;
            }

            byte[] bitmapArr = Base64.decode(data, Base64.DEFAULT);
            mProfilePicture = BitmapFactory.decodeByteArray(bitmapArr , 0, 0);

            cacheProfilePicture();

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private String getProfilePictureCachePath(){
        return mUsername + ".profile_picture.png";
    }

    private void loadProfilePicture(){
        if(mFacebookID != null){
            getFacebookProfilePicture();
        } else if(!getCachedPicture()){
            getUnisongProfilePicture();
        }
    }

    // writes the object using an output stream
    public void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(mUUID);
        out.writeObject(mPhoneNumber);
        out.writeObject(mProfilePictureCachePath);
        out.writeObject(mProfilePictureS3Key);
        out.writeObject(mName);
        out.writeObject(mFacebookID);
    }

    // reads an object using an output stream
    public void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
        mUUID = (UUID) in.readObject();
        mPhoneNumber = (String) in.readObject();
        mProfilePictureCachePath = (String) in.readObject();
        mProfilePictureS3Key = (String) in.readObject();
        mName = (String) in.readObject();
        mFacebookID = (String) in.readObject();
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

    private void getUserInfo(){
        Response response;
        try {
            if (mUsername != null) {
                response = mClient.get(NetworkUtilities.HTTP_URL + "/user/get-by-username/" + mUsername);
            } else if (mUUID != null) {
                response = mClient.get(NetworkUtilities.HTTP_URL + "/user/" + mUUID);
            } else if (mFacebookID != null) {
                response = mClient.get(NetworkUtilities.HTTP_URL + "/user/get-by-facebook/" + mFacebookID);
            } else {
                return;
            }
        } catch (IOException e){
            e.printStackTrace();
            return;
        }
        JSONObject object;
        try {
            object = new JSONObject(response.body().string());
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

}
