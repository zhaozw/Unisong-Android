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

    private byte mStreamId;

    private long mStartTime;

    public SongStartPacket(byte[] data){
        mData = data;
        decode();
    }


    public SongStartPacket(long songStartTime , byte streamId){
        byte[] data = new byte[]{CONSTANTS.SONG_START_PACKET_ID , streamId};

        byte[] startTime = ByteBuffer.allocate(8).putLong(songStartTime).array();

        //TODO: implement metadata like sample rate, song name, and whatever else is needed

        data = NetworkUtilities.combineArrays(data, startTime);


        mData = data;
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }


    @Override
    public byte getStreamId(){
        return mStreamId;
    }


    private void decode(){
        mStreamId = mData[1];

        byte[] packetIdArr = Arrays.copyOfRange(mData, 2, 6);

        mStartTime = ByteBuffer.wrap(packetIdArr).getInt();
    }

}
