package com.ezturner.speakersync.audio;

import com.ezturner.speakersync.network.NetworkUtilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioFrame {

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
    private Map<Integer, byte[]> mDatas;

    //The packet ID of the frame info packet
    private int mPacketId;

    //TODO: clean this class up and get rid of all of the unused stuff

    //The constructor without a play time
    public AudioFrame(byte[] data , int ID){
        mData = data;
        mID = ID;
    }

    //The constructor with a playtime
    public AudioFrame(byte[] data, int ID, long playTime, long length){
        this(data, ID);
        mPlayTime = playTime / 1000;
        mLength = length;
    }

    //The constructor for the AudioListener class, so it can be rebuilt one portion at a time
    public AudioFrame(int ID, int numPackets , long playTime , long length , int packetId){
        mID = ID;
        mNumPackets = numPackets;
        //Convert play time to milliseconds
        mPlayTime = playTime / 1000;
        mLength = length;
        mData = new byte[0];
        mNumPacketsAdded = 0;
        mPacketId = packetId;
        mDatas = new HashMap<Integer, byte[]>();
    }

    //Adds some data to recreate the original data
    public boolean addData( int packetId ,byte[] data ){
        mNumPacketsAdded++;

        mDatas.put(packetId , data);
        if(mNumPackets == mNumPacketsAdded){
            compileData();
            return true;
        }

        return false;
    }

    private void compileData(){
        for(int i = 1; i <= mNumPackets; i++){
            mData = NetworkUtilities.combineArrays(mData , mDatas.get(mPacketId + i));
        }
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
}
