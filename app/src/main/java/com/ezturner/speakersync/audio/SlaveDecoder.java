package com.ezturner.speakersync.audio;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by ezturner on 4/6/2015.
 */
public class SlaveDecoder {

    private String LOG_TAG = "SlaveDecoder";

    //The format that the MediaCodec will use
    private MediaFormat mFormat;
    private MediaCodec mCodec;

    private Thread mDecodeThread;

    private boolean mDecoding;

    private int mRuns;

    private Map<Integer , AudioFrame> mFrames;
    private int mCurrentFrame;
    private boolean mDecoderWaiting;

    public SlaveDecoder(String mime, int sampleRate , int channels , int bitrate){
        mFormat = new MediaFormat();
        mFormat.setString(MediaFormat.KEY_MIME , mime);
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

    public void addFrame(AudioFrame frame){
        synchronized (mFrames) {
            mFrames.put(frame.getID(), frame);
        }


    }

    private void decode() throws IOException{
        ByteBuffer[] codecInputBuffers  = mCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();

        final long kTimeOutUs = 1000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        Log.d(LOG_TAG, "Info size is : " + info.size);
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 10;


        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !mDecoding) {

            mRuns++;
            if(mRuns >= 200){
                System.gc();
                mRuns = 0;
            }

            if(!mFrames.containsKey(mCurrentFrame)){
                synchronized (mDecodeThread){
                    try {
                        mDecodeThread.wait();
                    } catch(InterruptedException e){
                        //This is called when .notify() is called
                    }
                }
            }

            synchronized (mFrames) {
                AudioFrame currentFrame = mFrames.get(mCurrentFrame);
                mCurrentFrame++;
            }



        }
    }
}
