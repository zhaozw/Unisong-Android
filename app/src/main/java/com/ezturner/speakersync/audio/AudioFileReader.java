package com.ezturner.speakersync.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;

import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.master.AudioBroadcaster;
import com.ezturner.speakersync.network.slave.AudioListener;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioFileReader {

    private static final String LOG_TAG = "AudioFileReader";

    private static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";

    //The current file that is being read from.
    private File mCurrentFile;

    //The bridge that handles communication to the broadcaster
    private ReaderBroadcasterBridge mBroadcasterBridge;

    //The object that handles the playback of audio data
    private TrackManagerBridge mTrackManagerBridge;

    //The listener that will sync the playback
    private AudioListener mListener;

    //The current ID of the audio frame
    private int mCurrentID;

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

    Handler handler = new Handler();

    String mime = null;
    int sampleRate = 0, channels = 0, bitrate = 0;
    long presentationTimeUs = 0, duration = 0;

    private Thread mDecodeThread;

    private boolean mRunning;

    public AudioFileReader(){
        mStop = false;
        mSampleTime = -67;
        mCurrentID = 0;
        mEvents = new AudioFileReaderEvents();
        mRunning = false;
    }

    public void setBridge(ReaderBroadcasterBridge bridge){
        mBroadcasterBridge = bridge;
    }

    public void readFile(String path) throws IOException{
        mCurrentFile = new File(path);
        mDecodeThread = getDecode();
        mDecodeThread.start();
        Log.d(LOG_TAG , "Started Decoding");
    }

    private Thread getDecode(){
        return new Thread(new Runnable()  {
            public void run() {
                try {
                    //decode();
                    decodeTest();
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

    private void decode() throws IOException {
        long startTime = System.currentTimeMillis();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        mRunning = true;
        // mExtractor gets information about the stream
        mExtractor = new MediaExtractor();
        // try to set the source, this might fail
        try {
            mExtractor.setDataSource(mCurrentFile.getPath());

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
            mTrackManagerBridge.createAudioTrack(sampleRate , channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO);
            // if duration is 0, we are probably playing a live stream
            duration = format.getLong(MediaFormat.KEY_DURATION);
            bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);

            mBroadcasterBridge.setAudioTrackInfo(sampleRate , channels , mime , duration , bitrate);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Reading format parameters exception: "+e.getMessage());
            e.printStackTrace();
            // don't exit, tolerate this error, we'll fail later if this is critical
            //TODO: figure out what to do with this error
        }
        Log.d(LOG_TAG, "Track info: mime:" + mime + " sampleRate:" + sampleRate + " channels:" + channels + " bitrate:" + bitrate + " duration:" + duration);

        // check we have audio content we know
        if (format == null || !mime.startsWith("audio/")) {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onError();  } });
            return;
        }
        // create the actual decoder, using the mime to select
        try {
            mCodec = MediaCodec.createDecoderByType(mime);
        } catch(IOException e){
            e.printStackTrace();
        }
        // check we have a valid codec instance
        if (mCodec == null) {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onError();  } });
            return;
        }

        //state.set(PlayerStates.READY_TO_PLAY);
        if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onStart(mime, sampleRate, channels, duration);  } });

        mCodec.configure(format, null, null, 0);
        mCodec.start();
        ByteBuffer[] codecInputBuffers  = mCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();

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

            mRuns++;
            if(mRuns >= 200){
                System.gc();
                mRuns = 0;
            }
            long playTime = -1;
            long length = -1;

            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            if (!sawInputEOS) {
                int inputBufIndex = mCodec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    int sampleSize = mExtractor.readSampleData(dstBuf, 0);
                    if (sampleSize < 0) {
                        Log.d(LOG_TAG, "saw input EOS. Stopping playback");
                        mBroadcasterBridge.lastPacket();
                        mTrackManagerBridge.lastPacket();
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {

                        length = mExtractor.getSampleTime() -presentationTimeUs;
                        playTime = mExtractor.getSampleTime();

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

                    Log.d(LOG_TAG , "Post-decode size :" + chunk.length);
                    createPCMFrame(chunk , playTime , length , true);
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
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = mCodec.getOutputBuffers();
                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = mCodec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + oformat);
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
        }

        mRunning = false;
        Log.d(LOG_TAG, "stopping...");

        releaseCodec();

        // clear source and the other globals
        //sourcePath = null;
        mSourceRawResId = -1;
        duration = 0;
        mime = null;
        sampleRate = 0; channels = 0; bitrate = 0;
        presentationTimeUs = 0; duration = 0;

        mStop = true;

        if(noOutputCounter >= noOutputCounterLimit) {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onError();  } });
        } else {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onStop();  } });
        }

        long finishTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Total time taken : " + (finishTime - startTime) / 1000 + " seconds");

    }


    //The general idea with this one is that instead of sending the AudioBroadcaster
    // the raw PCM/decoded data, we will send the mp3 data to decrease network load.
    public void decodeTest() throws IOException {
        long startTime = System.currentTimeMillis();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        // mExtractor gets information about the stream
        mExtractor = new MediaExtractor();
        // try to set the source, this might fail
        try {
            mExtractor.setDataSource(mCurrentFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            //TODO : Handle this exception
            //TODO: find out if this can fail with the MediaStore API
            return;
        }

        // Read track header
        MediaFormat format = getFormat();

        Log.d(LOG_TAG, "Track info: mime:" + mime + " sampleRate:" + sampleRate + " channels:" + channels + " bitrate:" + bitrate + " duration:" + duration);

        // check we have audio content we know
        if (format == null || !mime.startsWith("audio/")) {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onError();  } });
            return;
        }
        // create the actual decoder, using the mime to select
        try {
            mCodec = MediaCodec.createDecoderByType(mime);
        } catch(IOException e){
            e.printStackTrace();
        }

        // check we have a valid codec instance
        if (mCodec == null) {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onError();  } });
            return;
        }

        if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onStart(mime, sampleRate, channels, duration);  } });

        mCodec.configure(format, null, null, 0);
        mCodec.start();
        ByteBuffer[] codecInputBuffers  = mCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();

        mExtractor.selectTrack(0);

        // start decoding
        final long kTimeOutUs = 1000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 10;

        //Make sure mStop is false
        mStop = false;

        //The while loop that
        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !mStop) {

            checkGC();

            long playTime = -1;
            long length = -1;
            byte[] inBuf = null;

            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            if (!sawInputEOS) {
                int inputBufIndex = mCodec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    int sampleSize = mExtractor.readSampleData(dstBuf, 0);
                    if (sampleSize < 0) {
                        Log.d(LOG_TAG, "saw input EOS. Stopping playback");
                        mBroadcasterBridge.lastPacket();
                        mTrackManagerBridge.lastPacket();
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {

                        length = mExtractor.getSampleTime() - presentationTimeUs;
                        playTime = mExtractor.getSampleTime();

                        presentationTimeUs = mExtractor.getSampleTime();

                        //TODO: make sure that this is actually the way to get the real MP3 data
                        dstBuf.mark();
                        byte[] mp3Data = new byte[sampleSize];
                        dstBuf.get(mp3Data);
                        dstBuf.reset();


                        mCurrentID++;
                        createMP3Frame(mp3Data ,playTime , length);
                        final int percent =  (duration == 0)? 0 : (int) (100 * presentationTimeUs / duration);
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

                    //createPCMFrame(chunk , playTime , length, false);
                    if(inBuf != null)   createMP3Frame(inBuf , playTime , length);

                }
                try {
                    mCodec.releaseOutputBuffer(outputBufIndex, false);
                } catch(IllegalStateException e){
                    e.printStackTrace();
                }
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = mCodec.getOutputBuffers();
                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = mCodec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + oformat);
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
        }

        Log.d(LOG_TAG, "stopping...");

        releaseCodec();

        // clear source and the other globals
        //sourcePath = null;
        mSourceRawResId = -1;
        duration = 0;
        mime = null;
        sampleRate = 0; channels = 0; bitrate = 0;
        presentationTimeUs = 0; duration = 0;

        if(noOutputCounter >= noOutputCounterLimit) {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onError();  } });
        } else {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onStop();  } });
        }

        long finishTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Total time taken : " + (finishTime - startTime) / 1000 + " seconds");

        mRunning = false;

    }

    private int mTotalBytes = 0;
    private ArrayList<AudioFrame> mFrames= new ArrayList<AudioFrame>();

    private MediaFormat getFormat(){
        MediaFormat format = null;
        try {
            format = mExtractor.getTrackFormat(0);
            mime = format.getString(MediaFormat.KEY_MIME);
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            mTrackManagerBridge.createAudioTrack(sampleRate , channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO);
            // if duration is 0, we are probably playing a live stream
            duration = format.getLong(MediaFormat.KEY_DURATION);
            bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);

            mBroadcasterBridge.setAudioTrackInfo(sampleRate , channels , mime, duration , bitrate);
        } catch (Exception e){
            Log.e(LOG_TAG, "Reading format parameters exception: " + e.getMessage());
            e.printStackTrace();
            // don't exit, tolerate this error, we'll fail later if this is critical
            //TODO: figure out what to do with this error
        }

        return format;
    }

    private void releaseCodec(){
        if(mCodec != null) {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }

    private void checkGC(){
        mRuns++;
        if(mRuns >= 200){
            System.gc();
            mRuns = 0;
        }
    }

    //The code to stop the decoding
    public void stopDecode(){
        mStop = true;
    }

    private void createPCMFrame(byte[] data , long playTime, long length, boolean broadcast){
        AudioFrame frame;
        if(playTime != -1 && length != -1) {
            frame = new AudioFrame(data, mCurrentID, playTime, length);
        } else {
            frame = new AudioFrame(data , mCurrentID);
        }
        if(broadcast) {
            mBroadcasterBridge.addFrame(frame);
        }
        mTrackManagerBridge.addFrame(frame);

        mCurrentID++;

    }

    private void createMP3Frame(byte[] data, long playTime, long length){
        AudioFrame frame;
        if(playTime != -1 && length != -1) {
            frame = new AudioFrame(data, mCurrentID, playTime, length);
        } else {
            frame = new AudioFrame(data , mCurrentID);
        }

        mBroadcasterBridge.addFrame(frame);
    }

    public void setTrackManagerBridge(TrackManagerBridge bridge){
        mTrackManagerBridge = bridge;
    }

    public void setBroadcasterBridge(ReaderBroadcasterBridge bridge){
        mBroadcasterBridge = bridge;
    }



}
