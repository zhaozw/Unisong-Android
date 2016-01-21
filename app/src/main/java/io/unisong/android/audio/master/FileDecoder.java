package io.unisong.android.audio.master;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.Decoder;
import io.unisong.android.network.CONSTANTS;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ezturner on 5/20/2015.
 */
public class FileDecoder implements Decoder{

    private static final String LOG_TAG = FileDecoder.class.getSimpleName();

    private Map<Integer , AudioFrame> mFrames;
    private boolean mStop = false;

    //The file that is being read from.
    private File mCurrentFile;

    //The MediaExtractor that will handle the data extraction
    private MediaExtractor mExtractor;

    private Thread mDecodeThread;

    private boolean mRunning;
    private String mime = null;
    private int sampleRate = 0, channels = 0, bitrate = 0;
    private long presentationTimeUs = 0, duration = 0;

    //The media codec object used to decode the files
    private MediaCodec mCodec;

    //The time adjust for the PCM frames.
    private long mTimeAdjust = 0;

    //The current ID of the audio frame
    private Integer mCurrentFrameID;

    // The number of frames that will be buffered.
    private int mFrameBufferSize;

    private MediaFormat mInputFormat;
    private MediaFormat mOutputFormat;

    private AACEncoder mEncoder;

    public FileDecoder(String path){
        //Set the variables
        mRunning = false;

        // After hiatus: should I be instnatiating these here?
        mCurrentFrameID = 0;
        mSamples = 0l;

        mFrameBufferSize  = 20;

        //Create the file and start the Thread.
        mCurrentFile = new File(path);
    }

    /**
     * Starts the decoding at the beginning of the song.
     */
    public void startDecode(){
        getDecode().start();
    }

    public void startDecode(long seekTime){
        mSeekTime = seekTime;
        mTimeAdjust = seekTime;
        startDecode();
    }

    private Thread getDecode(){
        return new Thread(new Runnable()  {
            public void run() {
                try {
                    decode();
                } catch (IOException e){
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    public void setEncoder(AACEncoder encoder){
        mEncoder = encoder;
    }

    public void setFrameBufferSize(int size){
        mFrameBufferSize = size;
    }
    private long mSize;
    //A boolean telling us when the first Output format is changed, so that we can start the AAC Encoder
    private boolean mFirstOutputChange = true;
    private long mSeekTime;
    private Long mPlayTime = 0l;

    private void decode() throws IOException {
        mFrames = new HashMap<>();
        long startTime = System.currentTimeMillis();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        long startSampleTime = -1;
        mRunning = true;
        // mExtractor gets information about the stream
        mExtractor = new MediaExtractor();
        // try to set the source, this might fail
        try {
            mExtractor.setDataSource(mCurrentFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            //TODO : Handle this exception
            return;
        }

        Log.d(LOG_TAG , mCurrentFile.getPath());
        // Read track header
        try {
            mInputFormat = mExtractor.getTrackFormat(0);
            mime = mInputFormat.getString(MediaFormat.KEY_MIME);
            sampleRate = mInputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            channels = mInputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            // if duration is 0, we are probably playing a live stream
            duration = mInputFormat.getLong(MediaFormat.KEY_DURATION);
            if(mInputFormat.containsKey(MediaFormat.KEY_BIT_RATE))
                bitrate = mInputFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Reading format parameters exception: " + e.getMessage());
            e.printStackTrace();
            // don't exit, tolerate this error, we'll fail later if this is critical
        }
        Log.d(LOG_TAG, "Track info: mime:" + mime + " sampleRate:" + sampleRate + " channels:" + channels + " bitrate:" + bitrate + " duration:" + duration);


        // create the actual decoder, using the mime to select
        try {
            mCodec = MediaCodec.createDecoderByType(mime);
        } catch(IOException e){
            e.printStackTrace();
        }

        mCodec.configure(mInputFormat, null, null, 0);
        mCodec.start();
        ByteBuffer[] codecInputBuffers  = mCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();

        if(mSeekTime != 0) {
            mExtractor.seekTo((mSeekTime - 50) * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            Log.d(LOG_TAG, "mExtractor sample time is :" + mExtractor.getSampleTime() + " from SeekTime : " + mSeekTime);
        }
        mExtractor.selectTrack(0);

        // start decoding
        final long kTimeOutUs = 1000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        Log.d(LOG_TAG , "Info size is : "  + info.size);
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 25;

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
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {

                        mPlayTime = mExtractor.getSampleTime() / 1000;
//                        if(mSeekTime == 100000) Log.d(LOG_TAG , "PlayTime is : " + mPlayTime);

                        presentationTimeUs = mExtractor.getSampleTime();
                        final int percent =  (duration == 0)? 0 : (int) (100 * presentationTimeUs / duration);
//                        if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onPlayUpdate(percent, presentationTimeUs / 1000, duration / 1000);  } });
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

                    if(!mStop)  createPCMFrame(chunk);


                    waitForFrameUse();

                }

                try {
                    mCodec.releaseOutputBuffer(outputBufIndex, false);
                } catch(IllegalStateException e){
                    e.printStackTrace();
                }

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.d(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                    mStop = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                codecOutputBuffers = mCodec.getOutputBuffers();
                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                mOutputFormat = mCodec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + mOutputFormat);
                if(mEncoder != null)
                    mEncoder.setInputFormat(mOutputFormat);
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
        }


        mRunning = false;
        Log.d(LOG_TAG, "stopping...");

        long finishTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Total time taken : " + (finishTime - startTime) / 1000 + " seconds");
        Log.d(LOG_TAG , "Size is : " + mSize);

        Log.d(LOG_TAG , "mStop : "  +mStop);
        Log.d(LOG_TAG , "No Output Index: " + noOutputCounter);

        if(!mStop){
            // TODO : this is an interim fix. It'd be great to have a 'smarter' fault tolerance system
            Log.d(LOG_TAG, "We are stopping but mStop is false! Restarting at current time");
            this.startDecode(mPlayTime);
        }

        releaseCodec();
    }

    public void waitForFrameUse(){
        while(mFrames.size() > mFrameBufferSize){
            if(mStop){
                Log.d(LOG_TAG , "mStop is true, exiting loop.");
                break;
            }

            synchronized (this){
                try {
                    this.wait(5);
                } catch (InterruptedException e){

                }
            }
        }
    }

    private void releaseCodec(){
        if(mCodec != null) {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }

    public void destroy(){
        mStop = true;

        while(mRunning){}

        releaseCodec();
        mExtractor = null;
        mCurrentFile = null;

    }

    @Override
    public Map<Integer, AudioFrame> getFrames() {
        return mFrames;
    }

    @Override
    public void addInputFrame(AudioFrame frame) {
        synchronized (mFrames){
            mFrames.put(frame.getID() , frame);
        }
    }

    public boolean isRunning(){
        return mRunning;
    }

    private Long mSamples;

    private void createPCMFrame(byte[] data ){
        long playTime = (mSamples * 8000) / CONSTANTS.PCM_BITRATE + mTimeAdjust;

        AudioFrame frame = new AudioFrame(data, mCurrentFrameID, playTime);

        synchronized (mFrames) {
            mFrames.put(frame.getID(), frame);
        }

        mSamples += data.length;
        mCurrentFrameID++;
    }

    @Override
    public AudioFrame getFrame(int ID){
        AudioFrame frame;
        synchronized (mFrames){
            frame = mFrames.get(ID);
            mFrames.remove(ID);
        }
        return frame;
    }

    @Override
    public void seek(long seekTime) {
        mStop = true;
        while (mRunning){
            synchronized (this){
                try{
                    this.wait(1);
                } catch (InterruptedException e){
                    Log.d(LOG_TAG , "Thread interrupted while waiting in seek()");
                }
            }
        }
        mSeekTime = seekTime;
        mDecodeThread = getDecode();
        mDecodeThread.start();
    }

    @Override
    public boolean hasFrame(int ID) {
        synchronized (mFrames) {
            return mFrames.containsKey(ID);
        }
    }

    public MediaFormat getFormat(){
        return mInputFormat;
    }
}
