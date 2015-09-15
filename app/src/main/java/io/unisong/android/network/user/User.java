package io.unisong.android.network.user;

import android.util.Log;

import com.facebook.AccessToken;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;

/**
 * The class for containing user information.
 * Created by Ethan on 8/9/2015.
 */
public class User {

    private final static String LOG_TAG = User.class.getSimpleName();

    private final static byte USERNAME = 0;
    private final static byte PHONE_NUMBER = 1;
    private final static byte ID = 2;

    private HttpClient mClient;
    private String mUsername;
    private int mID;
    private String mPhoneNumber;
    private UUID mUUID;

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

    public String getPhoneNumber(){
        return mPhoneNumber;
    }

    /**
     * Encodes the information about this object that we wish to transmit
     * into a byte[] array. I am not currently using the serializable class
     * since I want to directly control the data that gets transmitted.
     * @return
     */
    public byte[] getBytes(){
        //Encode the username
        int usernameLength = getUsername().getBytes().length;
        byte[] header = new byte[]{USERNAME};
        byte[] length = ByteBuffer.allocate(4).putInt(usernameLength).array();

        byte[] data = NetworkUtilities.combineArrays(header, length);

        byte[] username = getUsername().getBytes();

        data = NetworkUtilities.combineArrays(data , username);

        //Encode the phone number
        int phoneLength = getPhoneNumber().getBytes().length;
        byte[] phoneHeaderArr = new byte[]{PHONE_NUMBER};
        byte[] phoneLengthArr = ByteBuffer.allocate(4).putInt(phoneLength).array();


        byte[] tempArr = NetworkUtilities.combineArrays(phoneHeaderArr , phoneLengthArr);

        byte[] phoneDataArr = getPhoneNumber().getBytes();

        tempArr = NetworkUtilities.combineArrays(tempArr , phoneDataArr);

        data = NetworkUtilities.combineArrays(data , tempArr);


        byte[] idHeaderArr = new byte[]{ID};
        byte[] idLengthArr = ByteBuffer.allocate(4).putInt(phoneLength).array();


        tempArr = NetworkUtilities.combineArrays(idHeaderArr , idLengthArr);

        data = NetworkUtilities.combineArrays(data, tempArr);
        return data;
    }

    public boolean equals(User user){
        if(user.getUsername().equals(mUsername) || user.getUUID().equals(mUUID)){
            return true;
        }
        return false;
    }
}
