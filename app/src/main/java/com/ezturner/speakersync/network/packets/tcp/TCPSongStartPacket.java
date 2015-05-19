package com.ezturner.speakersync.network.packets.tcp;

import android.util.Log;

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
public class TCPSongStartPacket {

    private final static String LOG_TAG = "TCPSongStartPacket";

    //The stream ID
    private byte mStreamID;

    //The time that the song will start at
    private long mSongStartTime;

    //The # of channels that will be used
    private int mChannels;



    //The constructor calls receive, which takes in data and populates the variables to be
    // returned with the getter methods.
    public TCPSongStartPacket(InputStream stream){
        receive(stream);
    }

    //The send method is static as it is merely a place to be able to modify both sides of the code easily.
    public static void send(OutputStream stream, long songStart, int channels , byte streamID){

        byte[] songStartArr = ByteBuffer.allocate(8).putLong(songStart).array();

        byte[] channelsArr = ByteBuffer.allocate(4).putInt(channels).array();

        songStartArr = NetworkUtilities.combineArrays(songStartArr, channelsArr);

        byte[] data = new byte[]{ streamID};

        data = NetworkUtilities.combineArrays(songStartArr, data);

        synchronized (stream) {
            try {
                stream.write(CONSTANTS.TCP_SONG_START);
                stream.write(data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }


    private void receive(InputStream stream){
        Log.d(LOG_TAG, "Receiving Song Start Instructions");
        byte[] data = new byte[13];

        synchronized (stream){
            try {
                NetworkUtilities.readFromStream(stream, data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        Log.d(LOG_TAG , "Stream Read succesfully");

        byte[] playTimeArr = Arrays.copyOfRange(data, 0, 8);

        mSongStartTime = ByteBuffer.wrap(playTimeArr).getLong();

        byte[] channelsArr = Arrays.copyOfRange(data, 8, 12);

        mChannels = ByteBuffer.wrap(channelsArr).getInt();

        mStreamID = data[12];

        Log.d(LOG_TAG , "mSongStartTime is: " + mSongStartTime + " and mChannels is : " + mChannels + " and mStreamID is : " + mStreamID);
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
}
