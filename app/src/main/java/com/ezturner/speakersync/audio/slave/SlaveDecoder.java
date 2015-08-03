package com.ezturner.speakersync.audio.slave;


import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.TimeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The parent class of SongDecoder, it maintains data and creates and
 * destroys SongDecoder instances as needed.
 * Handles the decoding of AAC data as needed by the AudioTrackManager class.
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

    //The adjust to playTime used to adjust time when we have done a seek command or when we have joined mid-session
    private long mTimeAdjust = 0;

    private long mOffset = 0;

    //The time that the song starts at
    private long mSongStartTime;

    private SongDecoder mSongDecoder;

    private TimeManager mTimeManager;

    private byte mStreamID;

    public SlaveDecoder(int channels , byte streamID){
        mCurrentID = 0;
        mFrames = new HashMap<>();
        mTimeManager = TimeManager.getInstance();
        mStreamID = streamID;

    }

    public void decode(int frame , long seekTime){
        mTimeAdjust = seekTime;
        setCurrentFrame(frame);

        //If slave codec is not null, then get rid of the old one
        if(mSongDecoder != null){
            mSongDecoder.stopDecode();
        }

//        mSongDecoder = new SongDecoder(this , channels , mFrames);
//        mSongDecoder.decode(frame);
    }

    public void setCurrentFrame(int currentFrame){
        mSongDecoder.setCurrentFrame(currentFrame);
    }


    //The number of AAC samples processed so far. Used to calculate play time.
    private long mSamples = 0;

    //Creates a frame out of PCM data and sends it to the AudioBroadcaster class.
    public void createFrame(byte[] data){
        long bitsProcessed = mSamples * 8000;
        long playTime = bitsProcessed  / CONSTANTS.PCM_BITRATE + mTimeAdjust;


        AudioFrame frame = new AudioFrame(data, mCurrentID, playTime , mStreamID);
        mCurrentID++;

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

    //TODO: keep this updated.
    public void destroy(){
        mSongDecoder.destroy();
        mSongDecoder = null;
        mTimeManager = null;

    }

    public void newSong(){
        if(mSongDecoder != null) {
            mSongDecoder.destroy();
        }
        mFrames = new HashMap<>();
        mSongDecoder = new SongDecoder(this, channels ,  mFrames);
    }

    private boolean mSeek = false;
    public void seek(long seekTime){
        mSongDecoder.destroy();
        mTimeAdjust = seekTime;
        mSongDecoder = new SongDecoder(this ,2, mFrames);
        mSeek = true;

        mSongDecoder.decode((int)(seekTime / (1024000.0 / 44100.0)));
    }
}
