package io.unisong.android.network.packets;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.NetworkUtilities;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Ethan on 4/2/2015.
 */
public class FramePacket implements NetworkPacket{

    private final String LOG_TAG = "FramePacket";

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

    //The datagram packet that was/will be sent through the network
    private DatagramPacket mPacket;

    //The AudioFrame this was made for
    private AudioFrame mFrame;

    //The data for the frame
    private byte[] mFrameData;


    public FramePacket(DatagramPacket packet){
        mPacket = packet;
        mData = packet.getData();
        decode();
    }

    //A constructor that takes in all of the relevant parameters, then formats the data
    //The song's start time is passed so we can calculate the direct playtime with the frame
    public FramePacket(AudioFrame frame, byte streamID, int packetID){
        mFrame = frame;
        mStreamID = streamID;
        mPacketID = packetID;

        //turn packet type into a byte array for combination , and put the stream ID in there
        byte[] packetType = new byte[]{CONSTANTS.UDP_FRAME_PACKET_ID, streamID};

        byte[] packetIDByte = ByteBuffer.allocate(4).putInt(packetID).array();

        //Throw the frame Id in a byte array, and throw all the other data in byte
        // arrays and combine them into the data that will go in the packet
        byte[] frameId = ByteBuffer.allocate(4).putInt(frame.getID()).array();

        byte[] dataSizeArr = ByteBuffer.allocate(4).putInt(frame.getData().length).array();

        byte[] data = NetworkUtilities.combineArrays(packetType, packetIDByte);

        data = NetworkUtilities.combineArrays(data , frameId);

        data = NetworkUtilities.combineArrays(data , dataSizeArr);

        mData = NetworkUtilities.combineArrays(data , frame.getData());

        mPacket = new DatagramPacket(mData, mData.length);
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

        byte[] dataLengthArr = Arrays.copyOfRange(mData , 10, 14);

        int dataLength = ByteBuffer.wrap(dataLengthArr).getInt();

//        Log.d(LOG_TAG , "Data Length is is : " + dataEnd + " so end position is: " + (dataEnd + 30) + "for frame #" + mFrameID);

        mData = Arrays.copyOfRange(mData , 14 ,dataLength + 14);
    }

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

    public int getFrameID(){
        return mFrameID;
    }

    public long getLength(){
        return mLength;
    }

    public AudioFrame getFrame(){
        return mFrame;
    }

    @Override
    public DatagramPacket getPacket() {
        return mPacket;
    }

    public String toString(){
        return "FramePacket#" + mPacketID;
    }

}