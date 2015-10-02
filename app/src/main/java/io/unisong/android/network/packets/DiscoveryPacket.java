package io.unisong.android.network.packets;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.User;

/**
 * Created by ezturner on 8/4/2015.
 */
public class DiscoveryPacket {

    private boolean mDecodeFailed;

    //The data for the packet that is being constructed
    private byte[] mData;
    private InetAddress mAddress;
    private User mUser;
    private UnisongSession mSession;

    private DatagramPacket mPacket;

    /**
     * This constructor will convert the Discovery Packet from the representation
     * on the wire to the actual JSON object used to transmit, and then will
     * assign values to all of the fields.
     * @param data
     */
    public DiscoveryPacket(byte[] data){
        mData = data;
        decode();
    }

    public DiscoveryPacket(InetAddress address, User user){
        mAddress = address;
        mUser = user;
        decode();
    }

    public DiscoveryPacket(InetAddress address, User user , UnisongSession session){
        mSession = session;
        mAddress = address;
        mUser = user;
        decode();
    }

    private void decode(){
        byte[] lengthArr = Arrays.copyOfRange(mData , 0 , 4);
        int length = ByteBuffer.wrap(lengthArr).getInt();

        // This will check for outlandish values that will be passed if we receive packets
        // from another app/protocol
        if(length < 0 || length > 5000){
            return;
        }

        byte[] stringArr = Arrays.copyOfRange(mData , 4 , mData.length);

        String jsonString = new String(stringArr);

        JSONObject object;
        try {
            object = new JSONObject(jsonString);
        } catch (Exception e){
            e.printStackTrace();
            return;
        }
    }

    private void encode(InetAddress address){
        // TODO : use UTF-8 for string encoding.

        JSONObject object = new JSONObject();

        try {
            object.put("IP", address.toString());
            object.put("userID" , mUser.getUUID());
            if(mSession != null){
                object.put("sessionID" , mSession.getSessionID());
            }
        } catch (JSONException e){
            e.printStackTrace();
            return;
        }
    }

    public DatagramPacket getPacket(){
        return mPacket;
    }

    public User getUser(){
        return mUser;
    }

    p
    public String toString(){
        return "DiscoveryPacket";
    }

    /**
     * Tells us whether the decoding failed
     * @return mDecodeFailed
     */
    public boolean getDecodeFailed(){
        return mDecodeFailed;
    }
}
