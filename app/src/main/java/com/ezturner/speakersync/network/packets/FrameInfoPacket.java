package com.ezturner.speakersync.network.packets;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by ezturner on 3/4/2015.
 */
public class FrameInfoPacket implements NetworkPacket{

    private byte[] mData;
    //The packet Id
    private int mPacketID;

    //The stream Id
    private byte mStreamID;

    //The Id for the frame being reconstructed
    private int mFrameID;

    //The time at which the frame is played/written
    private long mPlayTime;

    //The number of packets
    private int mNumPackets;

    //The length of this frame
    private long mLength;


    public FrameInfoPacket(byte[] data){
        mData = data;

    }

    //A constructor that takes in all of the relevant parameters, then formats the data
    public FrameInfoPacket(AudioFrame frame, int numPackets , byte streamId , long songStartTime , int packetId , long length){
    //turn packet type into a byte array for combination , and put the stream ID in there
        byte[] packetType = new byte[]{CONSTANTS.FRAME_INFO_PACKET_ID , streamId};

        byte[] packetIdByte = ByteBuffer.allocate(4).putInt(packetId).array();

        //Throw the frame Id in a byte array, and throw all the other data in byte
        // arrays and combine them into the data that will go in the packet
        byte[] frameId = ByteBuffer.allocate(4).putInt(frame.getID()).array();

        byte[] playTime = ByteBuffer.allocate(8).putLong(frame.getPlayTime() + songStartTime).array();

        byte[] numPacketsArr = ByteBuffer.allocate(4).putInt(numPackets).array();

        byte[] lengthArr = ByteBuffer.allocate(8).putLong(length).array();

        byte[] data = NetworkUtilities.combineArrays(packetType, packetIdByte);

        data = NetworkUtilities.combineArrays(data , frameId);

        data = NetworkUtilities.combineArrays(data , playTime);

        data = NetworkUtilities.combineArrays(data , numPacketsArr);

        data = NetworkUtilities.combineArrays(data , lengthArr);

        mData = data;
    }

    @Override
    public byte[] getData(){
        return mData;
    }

    private void decode(){

        byte[] packetIDArr = Arrays.copyOfRange(mData, 2, 6);

        mPacketID = ByteBuffer.wrap(packetIDArr).getInt();

        byte[] frameIDArr = Arrays.copyOfRange(mData, 6, 10);

        mFrameID = ByteBuffer.wrap(frameIDArr).getInt();

        byte[] playTimeArr = Arrays.copyOfRange(mData , 10 , 18);

        mPlayTime = ByteBuffer.wrap(playTimeArr).getLong();

        byte[] numPacketsArr = Arrays.copyOfRange(mData , 18 , 22);

        mNumPackets = ByteBuffer.wrap(numPacketsArr).getInt();

        byte[] lengthArr = Arrays.copyOfRange(mData , 22 , 30);

        mLength = ByteBuffer.wrap(lengthArr).getInt();

    }

    @Override
    public byte getStreamID(){
        return mStreamID;
    }

    public int getPacketID(){
        return mPacketID;
    }

    public long getPlayTime(){
        return mPlayTime;
    }

    public int getNumPackets(){
        return mNumPackets;
    }

    public int getFrameId(){
        return mFrameID;
    }

    public long getLength(){
        return mLength;
    }
}
