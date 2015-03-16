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

    //The time that the song will start on the Master device's clock
    private long mStartTime;

    private int mPacketID;

    private DatagramPacket mPacket;

    private int mChannels;

    //The sample rate for the AudioTrack
    private int mSampleRate;

    public SongStartPacket(byte[] data){
        mData = data;
        decode();
    }


    public SongStartPacket(long songStartTime , byte streamID  , int packetID , int sampleRate , int channels){
        byte[] data = new byte[]{CONSTANTS.SONG_START_PACKET_ID , streamID};

        byte[] packetIDArr = ByteBuffer.allocate(4).putInt(packetID).array();

        byte[] startTime = ByteBuffer.allocate(8).putLong(songStartTime).array();

        byte[] sampleRateArr = ByteBuffer.allocate(4).putInt(sampleRate).array();

        byte[] channelsArr = ByteBuffer.allocate(4).putInt(channels).array();

        //TODO: implement metadata like sample rate, song name, and whatever else is needed

        data = NetworkUtilities.combineArrays(data, packetIDArr);

        data = NetworkUtilities.combineArrays(data, startTime);

        data = NetworkUtilities.combineArrays(data, sampleRateArr);

        data = NetworkUtilities.combineArrays(data, channelsArr);

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

    public long getStartTime(){return mStartTime;}

    public int getPacketID(){return mPacketID;}

    public int getSampleRate(){return mSampleRate;}

    public int getChannels(){return mChannels;}

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

        byte[] sampleTimeArr = Arrays.copyOfRange(mData , 14, 18);

        mSampleRate = ByteBuffer.wrap(sampleTimeArr).getInt();

        byte[] channelsArr = Arrays.copyOfRange(mData , 18, 22);

        mChannels = ByteBuffer.wrap(channelsArr).getInt();
    }

}
