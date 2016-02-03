package io.unisong.android.audio;

import java.util.ArrayList;

import io.unisong.android.network.NetworkUtilities;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioFrame {

    private static final String LOG_TAG = "AudioFrame";

    private byte[] data;

    //The time that has been assigned for this frame to be written to the AudioTrack buffer.
    //is often null, until is set by setPlayTime(time);
    private long playTime;

    //The ID of the frame and associated packet
    private int ID;

    //The number of packets that will be used to reconstruct this frame
    private int mNumPackets;

    //The length of this time in microseconds
    private long length;

    //The number of data packets that have been added so far
    private int numPacketsAdded;

    //The temporary data storage used for the data packets
    private ArrayList<byte[]> datas;

    //The packet ID of the frame info packet
    private int packetID;

    //The stream that this frame belongs to
    private int songID;

    private short[] audioData;

    //The constructor, sets all of the data
    public AudioFrame(byte[] data, int ID, int songID){
        this.data = data;
        this.ID = ID;
        this.songID = songID;
    }

    public AudioFrame(byte[] data, int ID, long playTime){
        this.playTime = playTime;
        this.data = data;
        this.ID = ID;
    }

    public AudioFrame(byte[] data, int ID, long playTime , int songID){
        this.data = data;
        this.ID = ID;
        this.playTime = playTime;
        this.songID = songID;
    }

    //Adds some data to recreate the original data
    public boolean addData( int packetID ,byte[] data ){
        numPacketsAdded++;

        datas.set(packetID - this.packetID - 1, data);
        if(mNumPackets == numPacketsAdded){
            compileData();
            return true;
        }

        return false;
    }

    private void compileData(){
        for(int i = 0; i < datas.size(); i++){
            data = NetworkUtilities.combineArrays(data, datas.get(i));
        }
    }

    public short[] getAudioData(){
        return audioData;
    }
    //Getters and setters
    public void setPlayTime(long time){
        playTime = time;
    }

    public long getPlayTime(){
        return playTime;
    }

    public byte[] getData(){
        return data;
    }

    public int getID(){
        return ID;
    }

    public long getLengthMillis(){
        return length / 1000;
    }

    public int getSongID(){
        return songID;
    }

    public void setSongID(int songID){
        this.songID = songID;
    }

    public String toString(){
        return "Frame #" + getID() + " play time is : " + getPlayTime() + " and size is : " + data.length + " bytes";
    }
}
