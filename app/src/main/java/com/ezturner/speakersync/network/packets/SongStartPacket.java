package com.ezturner.speakersync.network.packets;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by ezturner on 3/4/2015.
 */
public class SongStartPacket implements NetworkPacket {


    private byte[] mData;

    private byte mStreamID;

    private long mStartTime;

    private int mPacketID;

    private DatagramPacket mPacket;

    public SongStartPacket(byte[] data){
        mData = data;
        decode();
    }


    public SongStartPacket(long songStartTime , byte streamID  , int packetID){
        byte[] data = new byte[]{CONSTANTS.SONG_START_PACKET_ID , streamID};

        byte[] packetIDArr = ByteBuffer.allocate(4).putInt(packetID).array();

        byte[] startTime = ByteBuffer.allocate(8).putLong(songStartTime).array();

        //TODO: implement metadata like sample rate, song name, and whatever else is needed

        data = NetworkUtilities.combineArrays(data, packetIDArr);

        data = NetworkUtilities.combineArrays(data, startTime);
        mData = data;
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }


    @Override
    public byte getStreamID(){
        return mStreamID;
    }

    public long getStartTime(){
        return mStartTime;
    }

    public int getPacketID(){
        return mPacketID;
    }

    @Override
    public DatagramPacket getPacket() {
        return mPacket;
    }

    @Override
    public void putPacket(DatagramPacket packet) {
        mPacket = packet;
    }


    private void decode(){
        mStreamID = mData[1];

        byte[] packetIdArr = Arrays.copyOfRange(mData, 2, 6);

        mPacketID = ByteBuffer.wrap(packetIdArr).getInt();

        byte[] playTimeArr = Arrays.copyOfRange(mData, 6, 14);

        mStartTime = ByteBuffer.wrap(playTimeArr).getLong();
    }

}
