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


    public SongStartPacket(long songStartTime , byte streamID  , int packetID , int sampleRate , int channels ,String mime , long duration , int bitrate ){
        byte[] data = new byte[]{CONSTANTS.SONG_START_PACKET_ID , streamID};

        byte[] packetIDArr = ByteBuffer.allocate(4).putInt(packetID).array();

        data = NetworkUtilities.combineArrays(data, packetIDArr);

        byte[] startTime = ByteBuffer.allocate(8).putLong(songStartTime).array();

        data = NetworkUtilities.combineArrays(data, startTime);

        byte[] sampleRateArr = ByteBuffer.allocate(4).putInt(sampleRate).array();

        data = NetworkUtilities.combineArrays(data, sampleRateArr);

        byte[] channelsArr = ByteBuffer.allocate(4).putInt(channels).array();

        data = NetworkUtilities.combineArrays(data, channelsArr);

        byte[] durationArr = ByteBuffer.allocate(8).putLong(duration).array();

        data = NetworkUtilities.combineArrays(data, durationArr);

        byte[] bitrateArr = ByteBuffer.allocate(4).putInt(bitrate).array();

        data = NetworkUtilities.combineArrays(data, bitrateArr);

        byte[] mimeArr = mime.getBytes(Charset.forName("UTF-8"));

        data = NetworkUtilities.combineArrays(data, mimeArr);
        //TODO: implement metadata like sample rate, song name, and whatever else is needed

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

        byte[] durationArr = Arrays.copyOfRange(mData , 22, 26);

        mDuration = ByteBuffer.wrap(channelsArr).getInt();

        byte[] bitrateArr = Arrays.copyOfRange(mData , 26, 30);

        mBitrate = ByteBuffer.wrap(channelsArr).getInt();

        byte[] mimeArr = Arrays.copyOfRange(mData , 30, mData.length);

        mMime = new String(mimeArr);
    }

}
