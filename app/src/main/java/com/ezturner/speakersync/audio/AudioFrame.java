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

    //TODO: clean this class up and get rid of all of the unused stuff

    //The constructor, sets all of the data
    public AudioFrame(byte[] data, int ID, long playTime){
        mData = data;
        mID = ID;
        mPlayTime = playTime;
    }

    public AudioFrame(byte[] data, int ID, long playTime, long length){
        mData = data;
        mID = ID;
        mPlayTime = playTime;
        mLength = length;
    }

    /*
    //The constructor with a playtime
    public AudioFrame(short[] data, int ID, long playTime, long length){
        mAudioData = data;
        mID = ID;
        mPlayTime = playTime / 1000;
        mLength = length;
    }*/

    /*//The constructor for the AudioListener class, so it can be rebuilt one portion at a time
    public AudioFrame(int ID, int numPackets , long playTime , long length , int packetID){
        mID = ID;
        mNumPackets = numPackets;
        //Convert play time to milliseconds
        mPlayTime = playTime / 1000;
        mLength = length;
        mData = new byte[0];
        mNumPacketsAdded = 0;
        mPacketID = packetID;
        mDatas = new ArrayList<byte[]>();
        for(int i = 0; i < numPackets; i++){
            mDatas.add(null);
        }
    }*/

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

    public void setOffset(long offset){
        mPlayTime += offset;
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

    public long getLength(){
        return mLength;
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
}
