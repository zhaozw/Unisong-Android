package com.ezturner.speakersync.network.packets;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Ethan on 4/23/2015.
 */
public class AudioDataPacket implements NetworkPacket {

    //The packet's raw data
    private byte[] mData;

    //The mp3 data
    private byte[] mAudioData;
    private DatagramPacket mPacket;
    private int mPacketID;
    private byte mStreamID;

    public AudioDataPacket(byte[] data){
        mData = data;
        decode();

    }

    public AudioDataPacket(byte[] data , byte streamID ,int packetID ){
        mPacketID = packetID;
        mStreamID = streamID;

        byte[] arr = new byte[]{CONSTANTS.AUDIO_DATA_PACKET_ID , streamID};

        byte[] packetIDArr = ByteBuffer.allocate(4).putInt(packetID).array();

        mData = NetworkUtilities.combineArrays(arr, packetIDArr);

        mData = NetworkUtilities.combineArrays(mData, data);

        mPacket = new DatagramPacket(mData, mData.length);
    }

    private void decode(){
        mStreamID = mData[1];

        byte[] packetArr = Arrays.copyOfRange(mData, 2, 6);

        mPacketID = ByteBuffer.wrap(packetArr).getInt();

        mAudioData = Arrays.copyOfRange(mData, 6, mData.length);
    }

    @Override
    public byte[] getData() {
        return mData;
    }

    public byte[] getAudioData(){
        return mAudioData;
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
}
