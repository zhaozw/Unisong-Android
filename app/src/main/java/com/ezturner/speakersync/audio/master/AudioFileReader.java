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
import java.util.List;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioFileReader {

    private static final String LOG_TAG = "AudioFileReader";

    private static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";

    //The current file that is being read from.
    private File mCurrentFile;

    //The object that handles the playback of audio data
    private TrackManagerBridge mTrackManagerBridge;

    //The bridge that will handle the AAC data being transmitted over the network
    private BroadcasterBridge mBroadcasterBridge;

    //The bridge that connects AudioFileReader to AACEncoder
    private ReaderToReaderBridge mAACBridge;

    //The listener that will sync the playback
    private AudioListener mListener;

    //The current ID of the audio frame
    private Integer mCurrentID;

    //Whether this is the first run
    private boolean mFirstRun;

    private long mSampleTime;

    //The MediaExtractor that will handle the data extraction
    private MediaExtractor mExtractor;

    //The event handler for the audio reading
    private AudioFileReaderEvents mEvents;

    //The media codec object used to decode the files
    private MediaCodec mCodec;

    private int mSourceRawResId = -1;
    private Context mContext;
    private boolean mStop = false;
    private byte mStreamID;

    String mime = null;
    int sampleRate = 0, channels = 0, bitrate = 0;
    long presentationTimeUs = 0, duration = 0;

    private Thread mDecodeThread;

    private boolean mRunning;

    private long mTimeAdjust = 0;
    //The encoder that turns the PCM data into AAC for transmit
    private AACEncoder mEncoder;


    public AudioFileReader(TrackManagerBridge trackManagerBridge) {
        mTrackManagerBridge = trackManagerBridge;
        mStop = false;
        mSampleTime = -67;
        mCurrentID = 0;
        mEvents = new AudioFileReaderEvents();
        mRunning = false;
        mSeekTime = 0;
        mSeekDone = true;
        mStreamID = -1;
    }

    public void readFile(String path) throws IOException{
        mStreamID++;
        if(mDecodeThread != null) {
            synchronized (mDecodeThread) {
                try {
                    mDecodeThread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mCurrentFile = new File(path);
        mDecodeThread = getDecode();
        mDecodeThread.start();
        Log.d(LOG_TAG , "Started Decoding");
    }

    private Thread getDecode(){
        return new Thread(new Runnable()  {
            public void run() {
                try {
                    decode();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
    }

    protected int inputBufIndex;
    protected int bufIndexCheck;
    protected int lastInputBufIndex;
    private int mRuns = 0;
    private boolean isRunning = false;

    private byte[] mData;

    private boolean mSeekDone;
    private int mCount = 0;
    private int mSize = 0;
    private NetworkInputStream mInputStream;
    //A boolean telling us when the first Output format is changed, so that we can start the AAC Encoder
    private boolean mFirstOutputChange = true;
    private long mSeekTime;
    private Long mPlayTime = 0l;

    private void decode() throws IOException {
        mInputStream = new NetworkInputStream();
        mData = new byte[0];
        long startTime = System.currentTimeMillis();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        long startSampleTime = -1;
        mSamples = 0l;
        mRunning = true;
        // mExtractor gets information about the stream
        mExtractor = new MediaExtractor();
        // try to set the source, this might fail
        try {
            mExtractor.setDataSource(mCurrentFile.getPath());
            mExtractor.seekTo(mSeekTime * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            if(mSeekTime != 0){
                mSeekTime = mExtractor.getSampleTime() / 1000;
                mCurrentID = (int) (mSeekTime / (1024000.0 / 44100.0));
                mSeekDone = true;
            }

            /* This is the code for using internal app resources
            if (mSourceRawResId != -1) {
                AssetFileDescriptor fd = mContext.getResources().openRawResourceFd(mSourceRawResId);
                mExtractor.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength());
                fd.close();
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            //TODO : Handle this exception
            return;
        }

        // Read track header
        MediaFormat format = null;
        try {
            format = mExtractor.getTrackFormat(0);
            mime = format.getString(MediaFormat.KEY_MIME);
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            // if duration is 0, we are probably playing a live stream
            duration = format.getLong(MediaFormat.KEY_DURATION);
            bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Reading format parameters exception: "+e.getMessage());
            e.printStackTrace();
            // don't exit, tolerate this error, we'll fail later if this is critical
            //TODO: figure out what to do with this error
        }
        Log.d(LOG_TAG, "Track info: mime:" + mime + " sampleRate:" + sampleRate + " channels:" + channels + " bitrate:" + bitrate + " duration:" + duration);


        // create the actual decoder, using the mime to select
        try {
            mCodec = MediaCodec.createDecoderByType(mime);
        } catch(IOException e){
            e.printStackTrace();
        }

        mCodec.configure(format, null, null, 0);
        mCodec.start();
        ByteBuffer[] codecInputBuffers  = mCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();


        mEncoder = new AACEncoder(mBroadcasterBridge , -1 , mStreamID);
        mAACBridge = new ReaderToReaderBridge(mEncoder);
        mExtractor.selectTrack(0);

        // start decoding
        final long kTimeOutUs = 1000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        Log.d(LOG_TAG , "Info size is : "  + info.size);
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 10;

        mStop = false;

        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !mStop) {


            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            if (!sawInputEOS) {
                int inputBufIndex = mCodec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0){
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    int sampleSize = mExtractor.readSampleData(dstBuf, 0);
                    if (sampleSize < 0){
                        Log.d(LOG_TAG, "saw input EOS. Stopping playback");
                        mTrackManagerBridge.lastPacket();
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {

                        mPlayTime = mExtractor.getSampleTime() / 1000;
//                        Log.d(LOG_TAG , "PlayTime is : " + mPlayTime);

                        presentationTimeUs = mExtractor.getSampleTime();
                        final int percent =  (duration == 0)? 0 : (int) (100 * presentationTimeUs / duration);
                        //if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onPlayUpdate(percent, presentationTimeUs / 1000, duration / 1000);  } });
                    }



                    mCodec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (!sawInputEOS) mExtractor.advance();

                } else {
                    //TODO: Investigate and reenable
                    //Log.e(LOG_TAG, "inputBufIndex " +inputBufIndex);
                }
            } // !sawInputEOS

            // decode to PCM and push it to the AudioTrack player
            int res = mCodec.dequeueOutputBuffer(info, kTimeOutUs);

            if (res >= 0) {
                if (info.size > 0)  noOutputCounter = 0;

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if(chunk.length > 0){

                    mSize += chunk.length;

                    createPCMFrame(chunk);
                	/*if(this.state.get() != PlayerStates.PLAYING) {
                		if (events != null) handler.post(new Runnable() { @Override public void run() { events.onPlay();  } });
            			state.set(PlayerStates.PLAYING);
                	}*/

                }
                try {
                    mCodec.releaseOutputBuffer(outputBufIndex, false);
                } catch(IllegalStateException e){
                    e.printStackTrace();
                }
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.d(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                codecOutputBuffers = mCodec.getOutputBuffers();
                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                MediaFormat oformat = mCodec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + oformat);
                if (mFirstOutputChange){
                    MediaFormat outFormat = mCodec.getOutputFormat();
                    int outputChannels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int outputSampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);

                    mFirstOutputChange = false;
                    Log.d(LOG_TAG, "Starting AAC Encoder now");


                    mBroadcasterBridge.setAudioInfo(outputChannels);
                    Log.d(LOG_TAG, "Output channels are : " + outputChannels);
                    mTrackManagerBridge.createAudioTrack(outputSampleRate , outputChannels);

                    //TODO: Ensure that the output format is always 2 channels and 44100 sample rate
                    mAACBridge.encode(format, 0);
                }
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }

        }


        mRunning = false;
        Log.d(LOG_TAG, "stopping...");

        // clear source and the other globals
        //sourcePath = null;
        mSourceRawResId = -1;
        duration = 0;
        mime = null;
        sampleRate = 0; channels = 0; bitrate = 0;
        presentationTimeUs = 0; duration = 0;

        mStop = true;

        long finishTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Total time taken : " + (finishTime - startTime) / 1000 + " seconds");
        Log.d(LOG_TAG , "Size is : " + mSize);

        releaseCodec();
    }


    private void releaseCodec(){
        if(mCodec != null) {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }

    //The code to stop the decoding
    public void
    stopDecode(){
        mStop = true;
    }

    private Long mSamples =0l;
    private void createPCMFrame(byte[] data ){
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
        mDecodeThread = null;
        mStop = true;
    }

    //TODO: put in code that recognizes if we have already decoded part of a song
    //Ends the current thread, and starts a new one
    public long seek(long seekTime){
        mSeekDone = false;
        mStop = true;

        while(mRunning){}

        mAACBridge.seek(mCurrentID , seekTime);
        mSeekTime = seekTime;
        mTimeAdjust = seekTime;

        mDecodeThread = getDecode();
        mDecodeThread.start();

        while(!mSeekDone){}

        return mSeekTime;
    }

}