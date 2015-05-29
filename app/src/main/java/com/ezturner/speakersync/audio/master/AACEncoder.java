package com.ezturner.speakersync.audio.master;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.BroadcasterBridge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ethan on 4/27/2015.
 */
public class AACEncoder {
    private static final String LOG_TAG = "AACEncoder";

    //The current ID of the audio frame
    private Integer mCurrentOutputID;

    //The media codec object used to decode the files
    private MediaCodec mCodec;

    private boolean mStop = false;
    private byte mStreamID;

    String mime = null;
    int sampleRate = 0, channels = 0, bitrate = 0;
    long presentationTimeUs = 0, duration = 0;

    private Thread mEncodeThread;

    private boolean mRunning;

    private InputStream mInputStream;
    private MediaFormat mInputFormat;

    private Map<Integer , AudioFrame> mFrames;

    private int mCurrentInputFrame = 0;

    //The last frame, a signal that the input is ending
    private int mLastFrame;

    //The Bridge that will be used to send the finished AAC frames to the broadcaster.
    private BroadcasterBridge mBroadcasterBridge;

    public AACEncoder(BroadcasterBridge bridge, int lastFrame , byte streamID, Map<Integer, AudioFrame> frames){

        mFrames = frames;

        if(frames.size() != 0){
            Log.d(LOG_TAG , "Setting mFrames, size is : " + mFrames.size());
        }

        mBroadcasterBridge = bridge;
        mCurrentOutputID = 0;
        mRunning = false;
        mLastFrame = lastFrame;
        mStreamID = streamID;
    }

    public void encode(MediaFormat inputFormat, int currentInputFrame , long startTime){
        mCurrentInputFrame = currentInputFrame;
        mCurrentOutputID = (int)(startTime / (1024000.0 / 44100.0));
        mInputFormat = inputFormat;
        mEncodeThread = getEncode();
        mEncodeThread.start();
    }

    private Thread getEncode(){
        return new Thread(new Runnable()  {
            public void run(){
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
    private boolean mSeek = false;
    private boolean isRunning = false;

    private int mOutputBitrate;
    private int mDataIndex = 0;


    private void decode() throws IOException {

        long startTime = System.currentTimeMillis();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        Log.d(LOG_TAG , "Creating mCodec : OMX.google.aac.encoder");
        // create the actual decoder, using the mime to select
        try{
            //TODO: see if this works on all devices.
            mCodec = MediaCodec.createByCodecName("OMX.google.aac.encoder");
        } catch(IOException e){
            e.printStackTrace();
        }
        // check we have a valid codec instance
        if (mCodec == null){
            Log.d(LOG_TAG , "mCodec is null ):");
            return;
        }

        mOutputBitrate = 1441200;

        channels = mInputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        sampleRate = mInputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        mime = "audio/mp4a-latm";
        bitrate = channels * 64000;

        Log.d(LOG_TAG , "InputFormat is : " + mInputFormat.toString());

        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, mime);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC); //fixed version
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64000 * channels);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channels);

        mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mCodec.start();
        ByteBuffer[] codecInputBuffers  = mCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();

        // start decoding
        final long kTimeOutUs = 1000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        Log.d(LOG_TAG , "Info size is : "  + info.size);
        //TODO: set sawInputEOS when we see the input EOS
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 10;

        mStop = false;


        int largestSize = 0;


        //TODO: deal with no data/end of stream
        while(!mStop){

            while(!mFrames.containsKey(mCurrentInputFrame)){

                if(mStop || mCurrentInputFrame == mLastFrame){
                    break;
                }
                try{
                    synchronized (this){
                        this.wait(10);
                    }
                } catch (InterruptedException e){
                    Log.d(LOG_TAG , "We have more frames to decode!");
                }

                if(mStop){
                    break;
                }

            }

            AudioFrame frame;
            synchronized (mFrames){
                frame = mFrames.get(mCurrentInputFrame);
            }
//            Log.d(LOG_TAG , frame.toString());
            long playTime = -1;
            long length = -1;

            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            if (mCurrentInputFrame != mLastFrame){
                int inputBufIndex = mCodec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0){
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    dstBuf.clear();

//                    Log.d(LOG_TAG , "DstBuf capacity: " + dstBuf.capacity() );
//                    Log.d(LOG_TAG , "DstBuf limit: " + dstBuf.limit());
//                    Log.d(LOG_TAG , "DstBuf Position: " + dstBuf.position());
                    int sampleSize = setData(frame , dstBuf);
//                    Log.d(LOG_TAG, "The Sample size is : " + sampleSize);


                    mCodec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                }
            } else {
                mStop = true;
            }

            // encode to AAC and then put in mFrames
            int outputBufIndex = mCodec.dequeueOutputBuffer(info, kTimeOutUs);


            if (outputBufIndex >= 0){
                if (info.size > 0)  noOutputCounter = 0;

                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

//                Log.d(LOG_TAG , "Output Buffer limit is : " + buf.limit());
                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if(chunk.length > 0){
                    createFrame(chunk);
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
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                codecOutputBuffers = mCodec.getOutputBuffers();
                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                MediaFormat outFormat = mCodec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + outFormat);
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
        }

        if(!mSeek){
            mBroadcasterBridge.lastFrame();
        } else {
            mSeek = false;
        }
        mRunning = false;

        Log.d(LOG_TAG, "stopping...");

        releaseCodec();

        // clear source and the other globals
        //sourcePath = null;
        duration = 0;
        mime = null;
        sampleRate = 0; channels = 0; bitrate = 0;
        presentationTimeUs = 0; duration = 0;

        mStop = true;
        long finishTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Total time taken : " + (finishTime - startTime) / 1000 + " seconds");

    }


    //Figures out how much data to put in the ByteBuffer dstBuf
    //TODO: Make this more efficient by getting rid of the byte array assignments and check if it makes a difference
    private int setData(AudioFrame frame , ByteBuffer dstBuf){
        byte[] data = frame.getData();
        int sampleSize = data.length;
//        Log.d(LOG_TAG , "Data size is: " + sampleSize + " mOldDataIndex: " + mDataIndex  + " , and currentFrame is " + mCurrentInputFrame);

        int spaceLeft = dstBuf.capacity() - dstBuf.position();


        //If we've got enough space for the whole rest of the frame, then use the rest of the frame
        if(spaceLeft > (sampleSize - mDataIndex) ){
            if(mDataIndex != 0) {
                data = Arrays.copyOfRange(data, mDataIndex, sampleSize);
                mDataIndex = 0;
            }
            mFrames.remove(frame.getID());
//            Log.d(LOG_TAG , "1: Data is : " + data.length);
            mCurrentInputFrame++;

        } else {
            //If not, then let's put what we can
            int endIndex = spaceLeft + mDataIndex;

            int startIndex = mDataIndex;
            //If the space left in the ByteBuffer is greater than the space left in the frame, then just put what's left
            if(endIndex >= data.length){
                endIndex = data.length;
                mCurrentInputFrame++;
                mDataIndex = 0;
            } else {
                mDataIndex = endIndex;
            }

            data = Arrays.copyOfRange(data, startIndex , endIndex);
//            Log.d(LOG_TAG , "2: Data is : " + data.length);
        }

        dstBuf.put(data);

        return data.length;
    }


    private void releaseCodec(){
        if(mCodec != null) {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }

    //The code to stop the decoding
    public void stopDecode(){
        mStop = true;
    }

    //The number of AAC samples processed so far. Used to calculate play time.
    private long mLastTime = 0;

    //Creates a frame out of PCM data and sends it to the AudioBroadcaster class.
    private void createFrame(byte[] data){
        //TODO: get rid of these and remove from the packets
        //TODO : figure out what this ^^ is referencing?
        long playTime = mLastTime;

        AudioFrame frame = new AudioFrame(data, mCurrentOutputID , mStreamID);
        mCurrentOutputID++;

        mBroadcasterBridge.addFrame(frame);

    }


    //This is called to add the frames to the input queue
    public void addFrames(ArrayList<AudioFrame> frames){
        synchronized (mFrames){
            for(AudioFrame frame : frames){
                if(!mFrames.containsKey(frame.getID())) {
                    mFrames.put(frame.getID(), frame);
                }
            }
        }
    }

    public void seek(){
        mStop = true;
        mSeek = true;
        long begin = System.currentTimeMillis();

        while (mRunning) {}

        Log.d(LOG_TAG , "Waiting for encode thread to finish , took " + (System.currentTimeMillis() - begin) + "ms");



    }

    public Map<Integer , AudioFrame> getFrames(){
        return mFrames;
    }

    public BroadcasterBridge getBroadcasterBridge(){
        return mBroadcasterBridge;
    }


    public void lastFrame(int currentID){
        mLastFrame = currentID;
    }

    public int getLastFrame(){
        return mLastFrame;
    }

    public void destroy(){
        mBroadcasterBridge.destroy();
    }
}
