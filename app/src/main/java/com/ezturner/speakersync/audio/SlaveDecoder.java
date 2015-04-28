package com.ezturner.speakersync.audio;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.ezturner.speakersync.Lame;
import com.ezturner.speakersync.network.slave.NetworkInputStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ezturner on 4/6/2015.
 */
public class SlaveDecoder {

    private static final int DEFAULT_FRAME_SIZE = 1152;
    private static final int INPUT_STREAM_BUFFER = 8192;
    private static final int MP3_SAMPLE_DELAY = 528;
    private static final int MP3_ENCODER_DELAY = 576;

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

    private int mLastFrame;

    private TrackManagerBridge mBridge;

    private boolean mIsRunning;

    //The input stream that will have the mp3 data
    private NetworkInputStream mInputStream;

    public SlaveDecoder(NetworkInputStream inputStream){
        mFrames = new HashMap();
        mInputStream = inputStream;
        mIsRunning = false;
    }

    public boolean isRunning(){
        return mIsRunning;
    }
    public void addBridge(TrackManagerBridge bridge){
        mBridge = bridge;
    }

    public void startDecode(){

        mDecodeThread = getDecodeThread();
        mDecodeThread.start();
    }

    private Thread getDecodeThread(){
        return new Thread(new Runnable()  {
            public void run() {

                synchronized (this){
                    try {
                        this.wait(750);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

//                try {
//                    decode();
//                } catch (IOException e){
//                    e.printStackTrace();
//                }
            }
        });
    }

    private int mLastAddedFrame  = 0;

    private void decode() throws IOException{


        mIsRunning = true;
        int ret = 0;
        ret = Lame.initializeDecoder();
        ret = Lame.configureDecoder(mInputStream);

        if (/* waveWriter != null && */ mInputStream!= null) {

            int samplesRead = 0, offset = 0;
            int skip_start = 0, skip_end = 0;
            int delay = Lame.getDecoderDelay();
            int padding = Lame.getDecoderPadding();
            int frameSize = Lame.getDecoderFrameSize();
            int totalFrames = Lame.getDecoderTotalFrames();



            int frame = 0;
            short[] leftBuffer = new short[DEFAULT_FRAME_SIZE];
            short[] rightBuffer = new short[DEFAULT_FRAME_SIZE];

            short[] result = new short[0];

            if (delay > -1 || padding > -1) {
                if (delay > -1) {
                    skip_start = delay + (MP3_SAMPLE_DELAY + 1);
                }
                if (padding > -1) {
                    skip_end = padding - (MP3_SAMPLE_DELAY + 1);
                }
            } else {
                skip_start = MP3_ENCODER_DELAY + (MP3_SAMPLE_DELAY + 1);
            }

            while(true) {

                if (!mInputStream.isReady()) {
                    Log.d(LOG_TAG, "Waiting 50ms to be ready");
                    synchronized (mDecodeThread) {
                        try {
                            mDecodeThread.wait(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    int oldSamplesRead = samplesRead;
                    samplesRead = Lame.decodeFrame(mInputStream, leftBuffer, rightBuffer);
                    offset = skip_start < samplesRead ? skip_start : samplesRead;
                    skip_start -= offset;
                    frame += samplesRead / frameSize;
                    long length = ((samplesRead - oldSamplesRead) / Lame.getDecoderBitrate()) * 1000;
                    long playTime = (oldSamplesRead / Lame.getDecoderBitrate()) * 1000;
                    if (samplesRead >= 0) {
                        if (skip_end > DEFAULT_FRAME_SIZE && frame + 2 > totalFrames) {
                            samplesRead -= (skip_end - DEFAULT_FRAME_SIZE);
                            skip_end = DEFAULT_FRAME_SIZE;
                        } else if (frame == totalFrames && samplesRead == 0) {
                            samplesRead -= skip_end;
                        }

                        if (Lame.getDecoderChannels() == 2) {
                            short[] combined = merge(leftBuffer, rightBuffer);

                            result = combineArrays(result, combined);
                            mBridge.addFrame(new AudioFrame(result, mCurrentFrame, length, playTime));

                            //waveWriter.write(leftBuffer, rightBuffer, offset, samplesRead);
                        } else {
                            result = combineArrays(result, leftBuffer);
                            mBridge.addFrame(new AudioFrame(result, mCurrentFrame, length, playTime));
                            //waveWriter.write(leftBuffer, offset, samplesRead);
                        }

                    } else {
                        break;
                    }
                }
            }

        }
    }

    short[] combineArrays(short[] a, short[] b){
        int aLen = a.length;
        int bLen = b.length;
        short[] c= new short[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    short[] merge(short[] a, short[] b)
    {
        assert (a.length == b.length);

        short[] result = new short[a.length + b.length];

        for (int i=0; i<a.length; i++)
        {
            result[i*2] = a[i];
            result[i*2+1] = b[i];
        }

        return result;
    }

    private void createPCMFrame(byte[] chunk, long length, long playTime, int ID){
        AudioFrame frame = new AudioFrame(chunk , ID , length, playTime);
        mBridge.addFrame(frame);
        Log.d(LOG_TAG , "PCM Frame created, of length: " + chunk.length);
    }
}
