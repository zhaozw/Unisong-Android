package com.ezturner.speakersync.network.packets;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by ezturner on 3/4/2015.
 */
public class SongStartPacket implements NetworkPacket {


    private byte[] mData;

    private byte mStreamID;

    //The time that the song will start on the Master device's clock
    private long mStartTime;

    private int mPacketID;

    private DatagramPacket mPacket;

    private int mChannels;

    //The sample rate for the AudioTrack
    private int mSampleRate;

    private String mMime;

    private long mDuration;

    private int mBitrate;

    public SongStartPacket(byte[] data){
        mData = data;
        decode();
    }


    public SongStartPacket(long songStartTime , byte streamID  , int packetID){
        mStreamID = streamID;

        byte[] data = new byte[]{CONSTANTS.SONG_START_PACKET_ID , streamID};

        byte[] packetIDArr = ByteBuffer.allocate(4).putInt(packetID).array();

        data = NetworkUtilities.combineArrays(data, packetIDArr);

        byte[] startTime = ByteBuffer.allocate(8).putLong(songStartTime).array();

        data = NetworkUtilities.combineArrays(data, startTime);
        //TODO: implement metadata like song name and whatever else is needed

        mData = data;

        mPacket = new DatagramPacket(mData , mData.length);
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }


    @Override
    public byte getStreamID(){
        return mStreamID;
    }

    public long getStartTime(){return mStartTime;}

    public int getPacketID(){return mPacketID;}

    @Override
    public DatagramPacket getPacket() {
        return mPacket;
    }


    private void decode(){
        mStreamID = mData[1];

        byte[] packetIdArr = Arrays.copyOfRange(mData, 2, 6);

        mPacketID = ByteBuffer.wrap(packetIdArr).getInt();

        byte[] playTimeArr = Arrays.copyOfRange(mData, 6, 14);

        mStartTime = ByteBuffer.wrap(playTimeArr).getLong();

    }

    public String toString(){
        return "SongStartPacket#"+ mPacketID +" for stream#" + mStreamID;
    }

}
