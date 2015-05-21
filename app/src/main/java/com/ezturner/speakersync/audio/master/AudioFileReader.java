package com.ezturner.speakersync.audio.master;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.ezturner.speakersync.audio.AudioFileReaderEvents;
import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.BroadcasterBridge;
import com.ezturner.speakersync.audio.ReaderToReaderBridge;
import com.ezturner.speakersync.audio.TrackManagerBridge;
import com.ezturner.speakersync.audio.master.AACEncoder;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.slave.AudioListener;
import com.ezturner.speakersync.network.slave.NetworkInputStream;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioFileReader {

    private static final String LOG_TAG = AudioFileReader.class.getSimpleName();

    private static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";

    //The object that handles the playback of audio data
    private TrackManagerBridge mTrackManagerBridge;

    //The bridge that will handle the AAC data being transmitted over the network
    private BroadcasterBridge mBroadcasterBridge;

    //The bridge that connects AudioFileReader to AACEncoder
    private ReaderToReaderBridge mAACBridge;

    //The current ID of the audio frame
    private Integer mCurrentID;

    //Whether this is the first run
    private boolean mFirstRun;

    private int mStartFrame;

    //The event handler for the audio reading
    private AudioFileReaderEvents mEvents;

    private byte mStreamID;

    private String mFilePath;

    private long mTimeAdjust = 0;

    private FileDecoder mDecoder;

    //The boolean telling us if a seek operation has been performed during this song
    private boolean mSeek;


    public AudioFileReader(TrackManagerBridge trackManagerBridge) {
        mTrackManagerBridge = trackManagerBridge;
        mCurrentID = 0;
        mEvents = new AudioFileReaderEvents();
        mStreamID = -1;
    }

    //Begins the process of reading and decoding a file.
    //Starts a FileDecoder to get ready.
    public void readFile(String path) throws IOException{
        //Set this for when we seek
        mFilePath = path;

        mSeek = false;
        mStreamID++;
        if(mDecoder != null){
            mDecoder.stop();
        }
        mStartFrame = 0;
        mSamples = 0l;
        mDecoder = new FileDecoder(path , this, 0l);
        Log.d(LOG_TAG , "Started Decoding");
    }




    public void createEncoder(){
        AACEncoder encoder = new AACEncoder(mBroadcasterBridge , -1 , mStreamID, new HashMap<Integer , AudioFrame>());
        mAACBridge = new ReaderToReaderBridge(encoder);
    }

    public void setEncoderFormat(MediaFormat format){
        if(!mSeek) {
            mAACBridge.encode(format, mStartFrame);
        }
    }




    //The code to stop the decoding
    public void stopDecode(){
        if(mDecoder != null)    mDecoder.stop();
    }

    private Long mSamples;
    public void createPCMFrame(byte[] data ){
        long playTime = (mSamples * 8000) / CONSTANTS.PCM_BITRATE + mTimeAdjust;
        long length = (data.length * 8000) / CONSTANTS.PCM_BITRATE;
//        if()
//        Log.d(LOG_TAG , "playTime is : " + playTime + " for #" + mCurrentID);
        AudioFrame frame = new AudioFrame(data, mCurrentID, playTime , mStreamID);
//        Log.d(LOG_TAG , "Frame is : " + frame.toString());

        mTrackManagerBridge.addFrame(frame);
        mAACBridge.addFrame(frame);

        mSamples += data.length;
        mCurrentID++;
    }


    public void setBroadcasterBridge(BroadcasterBridge bridge){
        mBroadcasterBridge = bridge;
    }

    public void destroy(){
        mBroadcasterBridge.destroy();
        mBroadcasterBridge = null;

        mAACBridge.destroy();
        mAACBridge = null;

        if(mDecoder != null){
            mDecoder.stop();
            mDecoder = null;
        }
    }


    //Disposes of the current FileDecoder and makes a new one.
    public void seek(long seekTime){

        mDecoder.stop();

        mSeek = true;

        mCurrentID = (int) (seekTime / (1024000.0 / 44100.0));
        mStartFrame = mCurrentID;

        mDecoder = new FileDecoder(mFilePath , this , seekTime);

        Log.d(LOG_TAG , "Got seek time from FileDecoder, is : " + seekTime);

        mAACBridge.seek(mCurrentID , seekTime , mStreamID);

        mTimeAdjust = seekTime;

    }

    public void lastPacket(){
        mTrackManagerBridge.lastPacket();
        mAACBridge.lastFrame(mCurrentID);
    }

    public void createAudioTrack(int outputSampleRate ,int outputChannels){
        mTrackManagerBridge.createAudioTrack(outputSampleRate , outputChannels);
    }

    public void setAudioInfo(int outputChannels){
        mBroadcasterBridge.setAudioInfo(outputChannels);
    }

}