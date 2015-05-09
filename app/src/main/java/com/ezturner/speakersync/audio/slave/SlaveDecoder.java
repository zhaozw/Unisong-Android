package com.ezturner.speakersync.audio.slave;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.ezturner.speakersync.Lame;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.TrackManagerBridge;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.TimeManager;
import com.ezturner.speakersync.network.slave.NetworkInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ezturner on 4/6/2015.
 */
public class SlaveDecoder {

    private final String LOG_TAG = "SlaveDecoder";

    //The current ID of the audio frame
    private Integer mCurrentID;

    String mime = null;
    int sampleRate = 0, channels = 0, bitrate = 0;
    long presentationTimeUs = 0, duration = 0;

    private Map<Integer , AudioFrame> mFrames;

    //The Bridge that will be used to send the finished AAC frames to the broadcaster.
    private TrackManagerBridge mTrackManagerBridge;

    //The adjustment for when we're joining mid-session
    private long mTimeAdjust = 0;

    private long mOffset = 0;

    //The time that the song starts at
    private long mSongStartTime;

    private int mOutputBitrate;

    private SlaveCodec mSlaveCodec;

    private TimeManager mTimeManager;

    public SlaveDecoder(TrackManagerBridge bridge , int channels, TimeManager manager){
        mTrackManagerBridge = bridge;
        mCurrentID = 0;
        mFrames = new HashMap<>();
        mTimeManager = manager;

        mSlaveCodec = new SlaveCodec(this, channels , mFrames, mTimeManager);

    }

    public void decode(int frame){
        setCurrentFrame(frame);

        //If slave codec is not null, then get rid of the old one
        if(mSlaveCodec != null){
            mSlaveCodec.stopDecode();
        }
        mSlaveCodec.decode(frame);
    }

    public void setCurrentFrame(int currentFrame){
        mSlaveCodec.setCurrentFrame(currentFrame);
        mTimeAdjust = (long) (currentFrame * (1024.0 / 44100.0) * 1000);
    }


    //The number of AAC samples processed so far. Used to calculate play time.
    private long mSamples = 0;

    //Creates a frame out of PCM data and sends it to the AudioBroadcaster class.
    public void createFrame(byte[] data){
        long bitsProcessed = mSamples * 8000;
        long playTime = bitsProcessed  / CONSTANTS.PCM_BITRATE + mTimeAdjust;


        AudioFrame frame = new AudioFrame(data, mCurrentID, playTime);
        mCurrentID++;

        mTrackManagerBridge.addFrame(frame);
        mSamples += data.length;

    }

    //This is called to add the frames to the input queue
    public void addFrames(ArrayList<AudioFrame> frames){
        synchronized (mFrames){
            for(AudioFrame frame : frames){
                mFrames.put(frame.getID(), frame);
            }
        }
    }

    public void setOffset(long offset){
        mOffset = offset;
    }

    public void setSongStartTime(long startTime){
        mSongStartTime = startTime;
    }

    public long getSongStartTime(){
        return mSongStartTime;
    }

    public long getOffset(){
        return mOffset;
    }
}
