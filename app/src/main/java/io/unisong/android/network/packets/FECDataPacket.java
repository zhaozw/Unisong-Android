package io.unisong.android.network.packets;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.NetworkUtilities;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Ethan on 5/15/2015.
 */
public class FECDataPacket implements NetworkPacket {
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


    public FECDataPacket(DatagramPacket packet){
        mPacket = packet;
        mData = packet.getData();
        decode();
    }

    //A constructor that takes in all of the relevant parameters, then formats the data
    //The song's start time is passed so we can calculate the direct playtime with the frame
    public FECDataPacket(byte streamID, int packetID, byte[] data){
        mStreamID = streamID;
        mPacketID = packetID;

        //turn packet type into a byte array for combination , and put the stream ID in there
        byte[] packetType = new byte[]{CONSTANTS.UDP_FEC_DATA_PACKET_ID, streamID};

        byte[] packetIDByte = ByteBuffer.allocate(4).putInt(packetID).array();

        //Throw the frame Id in a byte array, and throw all the other data in byte
        // arrays and combine them into the data that will go in the packet


        byte[] arrdata = NetworkUtilities.combineArrays(packetType, packetIDByte);


        data = NetworkUtilities.combineArrays(arrdata , data);

        mData = NetworkUtilities.combineArrays(arrdata , data);

        mPacket = new DatagramPacket(mData, mData.length);
    }

    @Override
    public byte[] getData(){
        return mData;
    }

    private void decode(){

        byte[] packetIDArr = Arrays.copyOfRange(mData, 2, 6);

        mPacketID = ByteBuffer.wrap(packetIDArr).getInt();

        mStreamID = mData[1];

        mData = Arrays.copyOfRange(mData , 6 , 1030);


    }


    @Override
    public DatagramPacket getPacket() {
        return mPacket;
    }

    @Override
    public int getPacketID(){
        return mPacketID;
    }

    public String toString(){
        return "FECDataPacket#" + mPacketID;
    }
}
