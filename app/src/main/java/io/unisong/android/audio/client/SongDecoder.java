package io.unisong.android.audio.client;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.Decoder;
import io.unisong.android.audio.master.AACEncoder;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.TimeManager;
import io.unisong.android.network.song.SongFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A decoder class that decodes a single song.
 * Created by ezturner on 5/6/2015.
 */
public class SongDecoder implements Decoder {

    private final String LOG_TAG = SongDecoder.class.getSimpleName();

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

    String mime = null;
    int sampleRate = 0, channels = 0, bitrate = 0;
    long presentationTimeUs = 0, duration = 0;

    private TimeManager mTimeManager;
    private Map<Integer , AudioFrame> mInputFrames;
    private Map<Integer , AudioFrame> mOutputFrames;

    private int mSamples;

    private long mTimeAdjust;

    private int mCurrentID;

    private int mLastFrameRequested;
    private int mFrameBufferSize;
    private SongFormat mInputFormat;



    public SongDecoder(SongFormat format){
        mInputFormat = format;
        mFrameBufferSize = 50;
        mOutputFrames = new HashMap<>();
        mInputFrames = new HashMap<>();
        mTimeManager = TimeManager.getInstance();
        mime = format.getMime();
        bitrate = format.getBitrate();
        sampleRate = format.getSampleRate();
        mRunning = false;
    }

    public void decode(long startTime){
        Log.d(LOG_TAG , "SongDecoder decode started at time: " + startTime);
        mCurrentID = (int) (startTime / 23.21995464852608);
        mCurrentFrame = 0;
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

        Log.d(LOG_TAG , "Setting up decodeMain()");

//        Log.d(LOG_TAG, "Creating mCodec : OMX.google.aac.decoder");
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
        //fixed version
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, mInputFormat.getSampleRate());
        format.setInteger(MediaFormat.KEY_BIT_RATE, mInputFormat.getBitrate());
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mInputFormat.getChannels());

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

        Log.d(LOG_TAG , "Starting decode loop.");
        //TODO: deal with no data/end of stream
        while(!mStop){

            waitForFrame(info.size);

            checkBuffer();

            if(mStop)   break;


            AudioFrame frame;
            synchronized (mInputFrames){
                frame = mInputFrames.get(mCurrentFrame);
                lastPlayTime = frame.getPlayTime() - mTimeManager.getOffset() + mTimeManager.getSongStartTime();
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
            //TODO: This throws an illegal state exception pretty often, try to fix it.
            // encode to AAC and then put in mInputFrames
            int outputBufIndex = 0;
            try {
                 outputBufIndex = mCodec.dequeueOutputBuffer(info, kTimeOutUs);
            } catch (IllegalStateException e){
                e.printStackTrace();
            }

            if (outputBufIndex >= 0){
                if (info.size > 0)  noOutputCounter = 0;

                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if(chunk.length > 0 && !mStop){
                    createPCMFrame(chunk);
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
        if(spaceLeft > (sampleSize - mDataIndex)){
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
        if(mCodec != null){
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }

    public void destroy(){
        mStop = true;
        while(mRunning){}
        mDecodeThread = null;
    }

    @Override
    public Map<Integer, AudioFrame> getFrames() {
        return mOutputFrames;
    }

    public void seek(long seekTime){
        mTimeAdjust = seekTime;
        mStop = true;
        while(mRunning){}

    }

    @Override
    public boolean hasFrame(int ID) {
        return mOutputFrames.containsKey(ID);
    }

    public void addInputFrame(AudioFrame frame){
        synchronized (mInputFrames){
            mInputFrames.put(frame.getID() , frame);
        }
    }

    private void createPCMFrame(byte[] data){
        long bitsProcessed = mSamples * 8000;
        long playTime = bitsProcessed  / CONSTANTS.PCM_BITRATE + mTimeAdjust;


        AudioFrame frame = new AudioFrame(data, mCurrentID, playTime);
        Log.d(LOG_TAG , "Frame #" + mCurrentID + " created.");
        mCurrentID++;

        mSamples += data.length;

        mOutputFrames.put(frame.getID() , frame);
    }

    public AudioFrame getFrame(int ID){
        mLastFrameRequested = ID > mLastFrameRequested ? ID : mLastFrameRequested;

        AudioFrame frame;
        synchronized (mOutputFrames){
            frame = mOutputFrames.get(ID);
            mOutputFrames.remove(ID);
        }
        return frame;
    }

    private void waitForFrame(int size){
        //TODO: Rewrite this to feed blank AAC frames instead of creating an empty PCM one
        while(!mInputFrames.containsKey(mCurrentFrame)){
            Log.d(LOG_TAG , "Current frame # is :" + mCurrentFrame + " and input frames size is : " + mInputFrames.size());
//                if(mStartFrame != 0)    Log.d(LOG_TAG , "Frame #" + mCurrentFrame + " not found.");
            if(mStop){
                break;
            }
            long diff = mTimeManager.getAACPlayTime(mCurrentFrame) - System.currentTimeMillis();

//                Log.d(LOG_TAG , "Diff is : " + diff + " for frame #" + mCurrentFrame);

            if(diff <= 50 && !mStop && false){

                Log.d(LOG_TAG , "Creating blank PCM frame for frame #" + mCurrentFrame + " which should be played in " + (diff)  + "ms" );

                createPCMFrame(new byte[size]);
                mCurrentFrame++;

                if(!mInputFrames.containsKey(mCurrentFrame)){
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
    }

    /**
     * Checks to see that we don't have too many frames decoded.
     * If we do, it'll wait until more are used up to decode additional ones.
     */
    private void checkBuffer(){
        while(mOutputFrames.size() > mFrameBufferSize){
            if(mStop)   return;
            synchronized (this){
                try{
                    this.wait(5);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean hasInputFrame(int ID){
        return mInputFrames.containsKey(ID);
    }

}