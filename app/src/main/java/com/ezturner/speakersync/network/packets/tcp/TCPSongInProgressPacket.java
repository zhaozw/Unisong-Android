package com.ezturner.speakersync.network.packets.tcp;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by ezturner on 5/5/2015.
 */
public class TCPSongInProgressPacket {

    //The stream ID
    private byte mStreamID;

    //The time that the song will start at
    private long mSongStartTime;

    //The # of channels that will be used
    private int mChannels;

    private int mCurrentPacket;


    //The constructor calls receive, which takes in data and populates the variables to be
    // returned with the getter methods.
    public TCPSongInProgressPacket(InputStream stream) throws IOException{
        receive(stream);
    }

    //Writes the Song In Progress information and identifier byte
    public static void send(OutputStream stream, Long startTime , int channels, int currentPacketID, byte streamID) throws IOException{
        byte[] startTimeArr = ByteBuffer.allocate(8).putLong(startTime).array();

        byte[] channelsArr = ByteBuffer.allocate(4).putInt(channels).array();

        startTimeArr = NetworkUtilities.combineArrays(startTimeArr, channelsArr);

        byte[] currentPacketArr = ByteBuffer.allocate(4).putInt(currentPacketID).array();

        byte[] data = new byte[]{streamID};

        startTimeArr = NetworkUtilities.combineArrays(startTimeArr, currentPacketArr);

        data = NetworkUtilities.combineArrays(startTimeArr, data);

        stream.write(CONSTANTS.TCP_SONG_IN_PROGRESS);
        stream.write(data);
    }

    private void receive(InputStream stream) throws IOException{
        byte[] data = new byte[17];

        synchronized (stream){
            stream.read(data);
        }
        byte[] playTimeArr = Arrays.copyOfRange(data, 0, 8);

        mSongStartTime = ByteBuffer.wrap(playTimeArr).getLong();

        byte[] channelsArr = Arrays.copyOfRange(data, 8, 12);

        mChannels = ByteBuffer.wrap(channelsArr).getInt();

        byte[] currentPacketArr = Arrays.copyOfRange(data, 12, 16);

        mCurrentPacket = ByteBuffer.wrap(channelsArr).getInt();

        mStreamID = data[16];
    }

    public byte getStreamID(){
        return mStreamID;
    }

    public long getSongStartTime(){
        return mSongStartTime;
    }

    public int getChannels(){
        return mChannels;
    }

    public int getCurrentPacket(){
        return mCurrentPacket;
    }
}
