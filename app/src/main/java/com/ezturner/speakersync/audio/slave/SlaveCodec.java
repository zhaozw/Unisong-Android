package com.ezturner.speakersync.audio.slave;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.TimeManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by ezturner on 5/6/2015.
 */
public class SlaveCodec {

    private final String LOG_TAG = SlaveCodec.class.getSimpleName();
    //The parent SlaveDecoder object that we will be sending our output data to
    private SlaveDecoder mDecoder;

    protected int inputBufIndex;
    protected int bufIndexCheck;
    protected int lastInputBufIndex;
    private boolean isRunning = false;

    private int mOutputBitrate;
    //The index for the read datas.
    private int mDataIndex = 0;

    private Thread mDecodeThread;

    //The media codec object used to decode the files
    private MediaCodec mCodec;

    private boolean mStop;

    private boolean mRunning;
    private Integer mCurrentFrame = 0;

    //The boolean telling the blank PCM frame creation code that we have a
    private boolean mSeek = false;

    String mime = null;
    int sampleRate = 0, channels = 0, bitrate = 0;
    long presentationTimeUs = 0, duration = 0;

    private TimeManager mTimeManager;
    private Map<Integer , AudioFrame> mFrames;


    public SlaveCodec(SlaveDecoder decoder, int channels, Map<Integer , AudioFrame> frames, TimeManager timeManager){
        mDecoder = decoder;
        mTimeManager = timeManager;
        mime = "audio/mp4a-latm";
        bitrate = channels * 64000;
        sampleRate = 44100;
        mFrames = frames;
        mRunning = false;
    }

    public void decode(int startFrame){
        mCurrentFrame = startFrame;
        mDecodeThread = getDecode();
        mDecodeThread.start();
    }

    private Thread getDecode(){
        return new Thread(new Runnable()  {
            public void run() {
                try {
                    decodeMain();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void decodeMain() throws IOException {

        long startTime = System.currentTimeMillis();


        Log.d(LOG_TAG, "Creating mCodec : OMX.google.aac.encoder");
        // create the actual decoder, using the mime to select
        try{
            //TODO: see if this works on all devices.
            mCodec = MediaCodec.createDecoderByType(mime);
        } catch(IOException e){
            e.printStackTrace();
        }
        // check we have a valid codec instance
        if (mCodec == null){
            Log.d(LOG_TAG , "mCodec is null ):");
            return;
        }




        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, mime);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC); //fixed version
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64000 * channels);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channels);

        mCodec.configure(format, null, null, 0);
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
        mRunning = true;

        boolean firstOutputChange = true;
        long lastPlayTime = System.currentTimeMillis();

        //TODO: deal with no data/end of stream
        while(!mStop){



            //TODO: test/check this out
            while(!mFrames.containsKey(mCurrentFrame)){

                if(mStop){
                    break;
                }
                long diff = mTimeManager.getAACPlayTime(mCurrentFrame) - System.currentTimeMillis();

                if(diff <= 50 && !mSeek){

                    Log.d(LOG_TAG , "Creating blank PCM frame for frame #" + mCurrentFrame + " which should be played in " + (diff)  + "ms" );

                    mDecoder.createFrame(new byte[info.size]);
                    mCurrentFrame++;

                    if(!mFrames.containsKey(mCurrentFrame)){
                        synchronized (this){
                            try{
                                this.wait(5);
                            } catch (InterruptedException e){

                            }
                        }
                    }


                } else {
                    synchronized (this){
                        try{
                            this.wait(20);
                        } catch (InterruptedException e){

                        }
                    }
                }
            }

            if(mSeek || mStop)   break;


            AudioFrame frame;
            synchronized (mFrames){
                frame = mFrames.get(mCurrentFrame);
                lastPlayTime = frame.getPlayTime() - mDecoder.getOffset() + mDecoder.getSongStartTime();
            }


            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            if (!sawInputEOS){
                int inputBufIndex = mCodec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0){
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    dstBuf.clear();

                    int sampleSize = setData(frame , dstBuf);

                    mCodec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                }
            }

            //TODO : check for illegal state exception?
            // encode to AAC and then put in mFrames
            int outputBufIndex = mCodec.dequeueOutputBuffer(info, kTimeOutUs);


            if (outputBufIndex >= 0){
                if (info.size > 0)  noOutputCounter = 0;

                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if(chunk.length > 0 && !mStop){
                    mDecoder.createFrame(chunk);
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
                mOutputBitrate = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) * outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) * 16 ;
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
        }

        Log.d(LOG_TAG, "stopping...");

        releaseCodec();
        mRunning = false;
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

        int spaceLeft = dstBuf.capacity() - dstBuf.position();


        //If we've got enough space for the whole rest of the frame, then use the rest of the frame
        if(spaceLeft > (sampleSize - mDataIndex) ){
            if(mDataIndex != 0) {
                data = Arrays.copyOfRange(data, mDataIndex, sampleSize);
                mDataIndex = 0;
            }
            mCurrentFrame++;

        } else {
            //If not, then let's put what we can
            int endIndex = spaceLeft + mDataIndex;

            int startIndex = mDataIndex;
            //If the space left in the ByteBuffer is greater than the space left in the frame, then just put what's left
            if(endIndex >= data.length){
                endIndex = data.length;
                mCurrentFrame++;
                mDataIndex = 0;
            } else {
                mDataIndex = endIndex;
            }

            data = Arrays.copyOfRange(data, startIndex , endIndex);
        }

        dstBuf.put(data);

        return data.length;
    }

    //The code to stop this MediaCodec
    public void stopDecode(){
        mStop = true;
    }

    private void releaseCodec(){
        if(mCodec != null) {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }

    public void setCurrentFrame(int currentFrame){
        mCurrentFrame = currentFrame;
    }

    public void destroy(){
        mDecodeThread = null;
        mStop = true;
    }

    public void seek(long seekTime){
        mStop = true;
        mSeek = true;
        long begin = System.currentTimeMillis();

    }
}
