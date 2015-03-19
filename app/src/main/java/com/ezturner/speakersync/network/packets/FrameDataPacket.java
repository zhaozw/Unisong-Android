package com.ezturner.speakersync.network.packets;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by ezturner on 3/4/2015.
 * This class handles the encoding and decoding of Frame data packets into/from byte arrays
 */
public class FrameDataPacket implements NetworkPacket {




    //The data for the packet that is being constructed
    private byte[] mData;

    //The fields that will be populated when the packet is decoded
    private int mPacketID;
    private byte mStreamID;
    private byte[] mAudioFrameData;

    private int mFrameID;

    private DatagramPacket mPacket;

    //The constructor for when you want to decode an incoming packet
    public FrameDataPacket(byte[] data){
        mData = data;
        decode();
    }

    public FrameDataPacket(byte[] data , byte streamID , int packetID , int frameID){
        mStreamID = streamID;
        mPacketID = packetID;
        mFrameID = frameID;
        mData = data;

        //turn packet type into a byte array for combination , and put the stream ID in there
        byte[] packetType = new byte[]{CONSTANTS.FRAME_DATA_PACKET_ID , streamID};

        //Convert the packet ID to byte for transmission.
        //TODO: Decide :Should this just be a two-byte value?
        byte[] packetIDByte = ByteBuffer.allocate(4).putInt(packetID).array();

        byte[] frameIDArr = ByteBuffer.allocate(4).putInt(frameID).array();

        //Combines the various byte arrays into
        packetType = NetworkUtilities.combineArrays(packetType, packetIDByte);

        mData = NetworkUtilities.combineArrays(packetType , frameIDArr);

        mData = NetworkUtilities.combineArrays(mData , data);

    }

    @Override
    public byte[] getData(){
        return mData;
    }

    private void decode(){

        mStreamID = mData[1];

        byte[] packetIDArr = Arrays.copyOfRange(mData, 2, 6);

        mPacketID = ByteBuffer.wrap(packetIDArr).getInt();

        byte[] frameIDArr = Arrays.copyOfRange(mData, 6, 10);

        mFrameID = ByteBuffer.wrap(frameIDArr).getInt();

        mAudioFrameData = Arrays.copyOfRange(mData, 10, mData.length);



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

    @Override
    public byte getStreamID(){
        return mStreamID;
    }

    public byte[] getAudioFrameData(){
        return mAudioFrameData;
    }

    public int getFrameID(){
        return mFrameID;
    }


}