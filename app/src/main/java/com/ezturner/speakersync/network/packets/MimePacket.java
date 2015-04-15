package com.ezturner.speakersync.network.packets;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by Ethan on 4/14/2015.
 */
public class MimePacket implements NetworkPacket {

    private byte[] mData;
    private byte mStreamID;
    private int mPacketID;
    private String mMime;

    private DatagramPacket mPacket;

    public MimePacket(byte[] data){
        mData = data;
        decode();
    }

    public MimePacket(String mime, int packetID, byte streamID){
        mPacketID = packetID;
        mMime = mime;
        byte[] data = new byte[]{CONSTANTS.MIME_PACKET_ID , streamID};

        byte[] packetIDarr = ByteBuffer.allocate(4).putInt(packetID).array();

        byte[] mimeArr = mime.getBytes(Charset.forName("UTF-8"));

        data = NetworkUtilities.combineArrays(data , packetIDarr);

        mData = NetworkUtilities.combineArrays(data , mimeArr);

        mPacket = new DatagramPacket(mData , mData.length);
    }


    @Override
    public byte[] getData() {
        return mData;
    }

    @Override
    public byte getStreamID() {
        return mStreamID;
    }

    @Override
    public int getPacketID() {
        return mPacketID;
    }

    @Override
    public DatagramPacket getPacket() {
        return mPacket;
    }

    public String getMime(){
        return mMime;
    }

    @Override
    public void putPacket(DatagramPacket packet) {
        mPacket = packet;
    }

    private void decode(){
        mStreamID = mData[1];

        byte[] packetIDArr = Arrays.copyOfRange(mData , 2 , 6);

        mPacketID = ByteBuffer.wrap(packetIDArr).getInt();

        byte[] mimeArr = Arrays.copyOfRange(mData , 6 , mData.length);

        mMime = new String(mimeArr);
    }
}
