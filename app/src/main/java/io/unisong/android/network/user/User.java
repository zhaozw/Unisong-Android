package io.unisong.android.network.user;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.facebook.AccessToken;
import io.unisong.android.network.http.HttpClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

/**
 * The class for containing user information.
 * Created by Ethan on 8/9/2015.
 */
public class User implements Serializable {

    private transient final static String LOG_TAG = User.class.getSimpleName();

    private transient AccessToken mFBAccessToken;
    private transient HttpClient mClient;
    private String mUsername;
    private String mPhoneNumber;
    private UUID mUUID;
    private String mProfilePictureCachePath;
    private String mProfilePictureS3Key;
    private String mName;

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
        mFBAccessToken = accessToken;
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

    public void setProfilePicture(ImageView view){
        mImageView = view;

        getSetImageThread().start();
    }

    private class DownloadFilesTask extends AsyncTask<URL, Integer, Long> {

        @Override
        protected Long doInBackground(URL... urls) {
            return null;
        }
    }


    private ImageView mImageView;


    private void getFacebookProfilePicture(){
        URL img_value = null;
        try {
            img_value = new URL("http://graph.facebook.com/" + mFBAccessToken.getUserId() + "/picture?type=small");
        } catch (MalformedURLException e){
            e.printStackTrace();
            return;
        }
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

    /**
     * Retrieves the Unisong profile picture for this user.
     */
    private void getUnisongProfilePicture(){
//        AmazonS3Client s3Client = new AmazonS3Client();

//        S3Object object = s3Client.getObject(new GetObjectRequest(
//                "unisongprofilepictures", mProfilePictureS3Key));

//        Bitmap bitmap = BitmapFactory.decodeStream(object.getObjectContent());

//        mImageView.setImageBitmap(bitmap);
    }

    private Thread getSetImageThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                if(mFBAccessToken != null){
                    getFacebookProfilePicture();
                } else if(!getCachedPicture()){
                    getUnisongProfilePicture();
                }
            }
        });
    }

    // writes the object using an output stream
    public void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(mUUID);
        out.writeObject(mPhoneNumber);
        out.writeObject(mProfilePictureCachePath);
        out.writeObject(mProfilePictureS3Key);
        out.writeObject(mName);
    }

    // reads an object using an output stream
    public void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
        mUUID = (UUID) in.readObject();
        mPhoneNumber = (String) in.readObject();
        mProfilePictureCachePath = (String) in.readObject();
        mProfilePictureS3Key = (String) in.readObject();
        mName = (String) in.readObject();
    }

}
