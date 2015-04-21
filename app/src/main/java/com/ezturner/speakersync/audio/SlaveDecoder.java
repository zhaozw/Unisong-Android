package com.ezturner.speakersync.audio;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ezturner on 4/6/2015.
 */
public class SlaveDecoder {

    private String LOG_TAG = "Decoder";

    //The format that the MediaCodec will use
    private MediaFormat mFormat;
    private MediaCodec mCodec;

    private Thread mDecodeThread;

    private boolean mDecoding;

    private int mRuns;

    private Map<Integer , AudioFrame> mFrames;
    private int mCurrentFrame;
    private boolean mDecoderWaiting;

    private int mLastFrame;

    private DecoderTrackManagerBridge mBridge;

    public SlaveDecoder(){
        mFrames = new HashMap();
    }

    public void initializeDecoder(String mime, int sampleRate, int channels, int bitrate){
        Log.d(LOG_TAG , "Initializing Decoding");
        mFormat = new MediaFormat();
        mFormat.setString(MediaFormat.KEY_MIME, mime);
        mFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        mFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT , channels);
        mFormat.setInteger(MediaFormat.KEY_BIT_RATE , bitrate);

        mDecoderWaiting = false;
        mDecoding = false;

        mCurrentFrame = 0;

        try {
            mCodec = MediaCodec.createDecoderByType(mime);
        } catch(IOException e){
            e.printStackTrace();
        }

        mCodec.configure(mFormat, null, null, 0);
        mCodec.start();
        mRuns = 0;

        startDecode();
    }
    public void addBridge(DecoderTrackManagerBridge bridge){
        mBridge = bridge;
    }

    public void startDecode(){
        if(!mDecoding) {
            mDecoding = true;
            mDecodeThread = getDecodeThread();
            mDecodeThread.start();
        }
    }
    private Thread getDecodeThread(){
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

    private int mLastAddedFrame  = 0;

    public void addFrames(ArrayList<AudioFrame> frames){
        synchronized (mFrames) {
            for(AudioFrame frame : frames){
                if(mLastAddedFrame < frame.getID()){
                    mLastAddedFrame = frame.getID();
                }
                mFrames.put(frame.getID() , frame);
            }
        }

        if(mDecodeThread != null) {
            synchronized (mDecodeThread) {
                mDecodeThread.notify();
            }
        }


    }

    public void lastPacket(){
        mLastFrame =mLastAddedFrame;
    }

    private int mFrameWaitingFor;
    private long mWaitForFrame;
    private void decode() throws IOException{
        mDecoding = true;
        ByteBuffer[] codecInputBuffers  = mCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();

        final long kTimeOutUs = 1000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        Log.d(LOG_TAG, "Info size is : " + info.size);
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 10;


        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && mDecoding ) {

            mRuns++;
            if (mRuns >= 200) {
                System.gc();
                mRuns = 0;
            }

            //TODO: Put in some code for skipping frames if necessary, and handling with slow/delayed frames
            if (!mFrames.containsKey(mCurrentFrame)) {
                synchronized (mDecodeThread) {
                    try {
                        mDecodeThread.wait();
                    } catch (InterruptedException e) {
                        //This is called when .notify() is called
                    }
                }
            } else {

                Log.d(LOG_TAG, "Grabbing frame");
                AudioFrame currentFrame = null;
                synchronized (mFrames) {
                    currentFrame = mFrames.get(mCurrentFrame);
                    mCurrentFrame++;
                }
                Log.d(LOG_TAG, "Got frame #" + currentFrame.getID());

                int inputBufIndex = mCodec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    byte[] sample = currentFrame.getData();
                    int sampleSize = sample.length;


                    Log.d(LOG_TAG, "Sample Size is : " + sampleSize);
                    dstBuf.put(currentFrame.getData());
//                Log.d(LOG_TAG , "Data is : " + Arrays.toString(currentFrame.getData()));

                    mCodec.queueInputBuffer(inputBufIndex, 0, sampleSize, currentFrame.getPlayTime(), sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                }

                int res = mCodec.dequeueOutputBuffer(info, kTimeOutUs);

                Log.d(LOG_TAG, "Output Buffer dequeued, res is : " + res);
                if (res >= 0) {
                    if (info.size > 0) noOutputCounter = 0;

                    int outputBufIndex = res;
                    ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                    final byte[] chunk = new byte[info.size];
                    buf.get(chunk);
                    buf.clear();


                    Log.d(LOG_TAG, "Chunk Set, is: " + chunk.length);
                    if (chunk.length > 0) {

                        createPCMFrame(chunk, currentFrame.getLength(), currentFrame.getPlayTime(), currentFrame.getID());
                	/*if(this.state.get() != PlayerStates.PLAYING) {
                		if (events != null) handler.post(new Runnable() { @Override public void run() { events.onPlay();  } });
            			state.set(PlayerStates.PLAYING);
                	}*/

                    }
                    try {
                        Log.d(LOG_TAG, "Trying to release output buffer");
                        mCodec.releaseOutputBuffer(outputBufIndex, false);
                        Log.d(LOG_TAG, "Output Buffer Released");
                    } catch (IllegalStateException e) {
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


                Log.d(LOG_TAG, "End of loop");
                if (mCurrentFrame == mLastFrame) {
                    mDecoding = false;
                }
            }
        }
    }

    private void createPCMFrame(byte[] chunk, long length, long playTime, int ID){
        AudioFrame frame = new AudioFrame(chunk , ID , length, playTime);
        Log.d(LOG_TAG , "PCM Frame created, of length: " + chunk.length);
    }
}