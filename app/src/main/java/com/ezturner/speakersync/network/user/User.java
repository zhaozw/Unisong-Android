package com.ezturner.speakersync.network.user;

import com.ezturner.speakersync.network.NetworkUtilities;

import net.fec.openrq.util.io.ByteBuffers;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by Ethan on 8/9/2015.
 */
public class User {

    private final static byte USERNAME = 0;
    private final static byte PHONE_NUMBER = 1;

    private String mUsername;
    private String mPhoneNumber;

    public User(String username, String phonenumber){
        mUsername = username;
        mPhoneNumber = phonenumber;
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

        return data;
    }
}