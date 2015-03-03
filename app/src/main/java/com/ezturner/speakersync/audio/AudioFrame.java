package com.ezturner.speakersync.audio;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioFrame {

    private byte[] mData;

    //The time that has been assigned for this frame to be written to the AudioTrack buffer.
    //is often null, until is set by setPlayTime(time);
    private long mWriteTime;

    //The ID of the frame and associated packet
    private int mID;

    //The constructor without a play time
    public AudioFrame(byte[] data , int ID){
        mData = data;
        mID = ID;
    }

    //The constructor with a playtime
    public AudioFrame(byte[] data, int ID, long playTime, long length){
        this(data, ID);
        mWriteTime = playTime;
    }

    //Getters and setters
    public void setPlayTime(long time){
        mWriteTime = time;
    }

    public long getPlayTime(){
        return mWriteTime;
    }

    public byte[] getData(){
        return mData;
    }

    public int getID(){
        return mID;
    }

}
