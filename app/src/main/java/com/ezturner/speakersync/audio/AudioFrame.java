package com.ezturner.speakersync.audio;

import android.util.Log;

import com.ezturner.speakersync.network.NetworkUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioFrame {

    private static final String LOG_TAG = "AudioFrame";

    private byte[] mData;

    //The time that has been assigned for this frame to be written to the AudioTrack buffer.
    //is often null, until is set by setPlayTime(time);
    private long mPlayTime;

    //The ID of the frame and associated packet
    private int mID;

    //The number of packets that will be used to reconstruct this frame
    private int mNumPackets;

    //The length of this time in microseconds
    private long mLength;

    //The number of data packets that have been added so far
    private int mNumPacketsAdded;

    //The temporary data storage used for the data packets
    private ArrayList<byte[]> mDatas;

    //The packet ID of the frame info packet
    private int mPacketID;

    //The stream that this frame belongs to
    private byte mStreamID;

    private short[] mAudioData;

    //The constructor, sets all of the data
    public AudioFrame(byte[] data, int ID, byte streamID){
        mData = data;
        mID = ID;
        mStreamID = streamID;
    }

    public AudioFrame(byte[] data, int ID, long playTime){
        mPlayTime = playTime;
        mData = data;
        mID = ID;
    }

    public AudioFrame(byte[] data, int ID, long playTime , byte streamID){
        mData = data;
        mID = ID;
        mPlayTime = playTime;
        mStreamID = streamID;
    }

    //Adds some data to recreate the original data
    public boolean addData( int packetID ,byte[] data ){
        mNumPacketsAdded++;

        mDatas.set(packetID - mPacketID - 1, data);
        if(mNumPackets == mNumPacketsAdded){
            compileData();
            return true;
        }

        return false;
    }

    private void compileData(){
        for(int i = 0; i < mDatas.size(); i++){
            mData = NetworkUtilities.combineArrays(mData , mDatas.get(i));
        }
    }

    public short[] getAudioData(){
        return mAudioData;
    }
    //Getters and setters
    public void setPlayTime(long time){
        mPlayTime = time;
    }

    public long getPlayTime(){
        return mPlayTime;
    }

    public byte[] getData(){
        return mData;
    }

    public int getID(){
        return mID;
    }

    public long getLengthMillis(){
        return mLength / 1000;
    }

    public byte getStreamID(){
        return mStreamID;
    }

    public void setStreamID(byte streamID){
        mStreamID = streamID;
    }

    public String toString(){
        return "Frame #" + getID() + " play time is : " + getPlayTime() + " and size is : " + mData.length + " bytes";
    }
}
