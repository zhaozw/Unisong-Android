package com.ezturner.speakersync.network.packets;

import com.ezturner.speakersync.MyApplication;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Ethan on 3/5/2015.
 */
public class MasterResponsePacket implements NetworkPacket {
    //The data for the packet that is being constructed
    private byte[] mData;

    //The fields that will be populated when the packet is decoded
    private int mPacketID;
    private byte[] mAudioFrameData;

    private int mFrameID;

    private byte mStreamID;

    private int mPort;

    //The constructor for when you want to decode an incoming packet
    public MasterResponsePacket(byte[] data){
        mData = data;
        decode();
    }

    public MasterResponsePacket(int port){
        //Set the packet type
        byte[] data = new byte[CONSTANTS.MASTER_RESPONSE_PACKET];

        //Get port
        data = NetworkUtilities.combineArrays( data , ByteBuffer.allocate(4).putInt(port).array());

        //Get phone number TODO: implement
        //byte[] number = MyApplication.getPhoneNumber().getBytes();

        //combine the two arrays
        mData = data;//NetworkUtilities.combineArrays(data);
    }

    @Override
    public byte[] getData(){
        return mData;
    }

    private void decode(){
        byte[] playTimeArr = Arrays.copyOfRange(mData , 1 , 5);

        mPort = ByteBuffer.wrap(playTimeArr).getInt();
    }

    @Override
    public byte getStreamID(){
        return mStreamID;
    }

    public int getPort(){
        return mPort;
    }
}
