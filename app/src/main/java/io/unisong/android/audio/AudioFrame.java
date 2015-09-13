package io.unisong.android.audio;

import io.unisong.android.network.NetworkUtilities;

import java.util.ArrayList;

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
    private int mSongID;

    private short[] mAudioData;

    //The constructor, sets all of the data
    public AudioFrame(byte[] data, int ID, int songID){
        mData = data;
        mID = ID;
        mSongID = songID;
    }

    public AudioFrame(byte[] data, int ID, long playTime){
        mPlayTime = playTime;
        mData = data;
        mID = ID;
    }

    public AudioFrame(byte[] data, int ID, long playTime , int songID){
        mData = data;
        mID = ID;
        mPlayTime = playTime;
        mSongID = songID;
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

    public int getSongID(){
        return mSongID;
    }

    public void setSongID(int songID){
        mSongID = songID;
    }

    public String toString(){
        return "Frame #" + getID() + " play time is : " + getPlayTime() + " and size is : " + mData.length + " bytes";
    }
}
