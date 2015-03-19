package com.ezturner.speakersync.network.packets;

import android.util.Log;

import com.ezturner.speakersync.MyApplication;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Ethan on 3/5/2015.
 */
public class MasterResponsePacket implements NetworkPacket {
    //The data for the packet that is being constructed
    private byte[] mData;

    //The fields that will be populated when the packet is decoded
    private byte[] mAudioFrameData;

    private int mFrameID;

    private byte mStreamID;

    private int mPort;

    private DatagramPacket mPacket;

    //The constructor for when you want to decode an incoming packet
    public MasterResponsePacket(byte[] data){
        mData = data;
        decode();
    }

    public MasterResponsePacket(int port){
        //Set the packet type
        byte[] data = new byte[]{CONSTANTS.MASTER_RESPONSE_PACKET};

        byte[] portArr =  ByteBuffer.allocate(4).putInt(port).array();
        //Get port
        data = NetworkUtilities.combineArrays( data , portArr);
        //Get phone number TODO: implement phone number shit
        //byte[] number = MyApplication.getPhoneNumber().getBytes();

        //combine the two arrays
        mData = data;//NetworkUtilities.combineArrays(data);
    }


    @Override
    public byte[] getData(){
        return mData;
    }

    private void decode(){
        byte[] portArr = Arrays.copyOfRange(mData , 1 , 5);

        mPort = ByteBuffer.wrap(portArr).getInt();
    }

    @Override
    public byte getStreamID(){
        return mStreamID;
    }

    public int getPort(){
        return mPort;
    }

    public int getPacketID(){
        return -1;
    }

    @Override
    public DatagramPacket getPacket() {
        return mPacket;
    }

    @Override
    public void putPacket(DatagramPacket packet) {
        mPacket = packet;
    }
}