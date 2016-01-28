package io.unisong.android.audio.master;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.song.LocalSong;

/**
 * Takes in PCM data and encodes to AAC
 * Created by Ethan on 4/27/2015.
 */
public class AACEncoder{
    private static final String LOG_TAG = AACEncoder.class.getSimpleName();

    //The current ID of the audio frame
    private int mCurrentOutputID;

    //The media codec object used to encode the files
    private MediaCodec mCodec;

    private boolean mStop, mKeepFrames;
    private int mSongID;

    String mime = null;
    int sampleRate = 0, channels = 0, bitrate = 0;
    long presentationTimeUs = 0, duration = 0;

    private Thread mEncodeThread;

    private boolean mRunning;

    private LocalSong mSong;
    private MediaFormat mInputFormat;

    //The output and input frames
    private Map<Integer , AudioFrame> mOutputFrames;
    private Map<Integer, AudioFrame> mInputFrames;


    private int mCurrentInputFrameID = 0;

    //The last frame, a signal that the input is ending
    private int mLastFrame;

    //The FileDecoder that turns our source file into
    private FileDecoder mDecoder;

    //The highest frame # used.
    private int mHighestFrameUsed;
    private int mFrameBufferSize;
    private boolean mRemoveFrames;

    public AACEncoder(){
        mFrameBufferSize = 20;
        mStop = false;
        mKeepFrames = true;
        mHighestFrameUsed = 0;

        mInputFrames = new TreeMap<>();
        mOutputFrames = new TreeMap<>();

        mLastFrame = -1;

        mCurrentOutputID = 0;
        mCurrentInputFrameID = 0;
        mRunning = false;
    }

    public void setBufferSize(int size){
        mFrameBufferSize = size;
    }

    /**
     * Tell the AACEncoder whether to remove frames after they are used.
     * Set to false by default.
     */
    public void setRemoveFrames(boolean remove){
        mRemoveFrames = remove;
    }

    public void encode(long startTime, int songID, String filePath){
        try {
            mSongID = songID;
            mCurrentOutputID = (int) (startTime / (1024000.0 / 44100.0));
            mDecoder = new FileDecoder(filePath);
            mDecoder.setEncoder(this);
            mDecoder.startDecode(startTime);
            mEncodeThread = getEncode();
            mEncodeThread.start();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private Thread getEncode(){
        return new Thread(new Runnable()  {
            public void run(){
                try {
                    encode();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
    }

    protected int inputBufIndex;
    protected int bufIndexCheck;
    protected int lastInputBufIndex;
    private boolean mSeek = false;
    private boolean isRunning = false;

    private int mOutputBitrate;
    private int mDataIndex = 0;


    private void encode() throws IOException {

        // TODO : get rid of frames that have been played.
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

        // If mInputFormat is null, then lets wait until it is no longer null
        while (mInputFormat == null){
            try {
                synchronized (this){
                    this.wait(2);
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        channels = 2;
        sampleRate = 44100;
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

        //mSong.setFormat(format);

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


            while(!mInputFrames.containsKey(mCurrentInputFrameID)){

                if(mStop || mCurrentInputFrameID == mLastFrame){
                    break;
                }
                try{
                    synchronized (this){
                        this.wait(10);
                    }
                } catch (InterruptedException e){
                    Log.d(LOG_TAG , "We have more frames to encode!");
                }

                if(mStop){
                    break;
                }

                if(mDecoder.hasFrame(mCurrentInputFrameID)){
                    mInputFrames.put(mCurrentInputFrameID , mDecoder.getFrame(mCurrentInputFrameID));
                }


            }


            while(mHighestFrameUsed + mFrameBufferSize <= mCurrentOutputID){
                try{
                    synchronized (this){
                        this.wait(10);
                    }
                } catch (InterruptedException e){

                }

            }


            AudioFrame frame;
            synchronized (mInputFrames){
                frame = mInputFrames.get(mCurrentInputFrameID);
            }

//            Log.d(LOG_TAG , frame.toString());
            long playTime = -1;
            long length = -1;

            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            if (mCurrentInputFrameID != mLastFrame){
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


            // encode to AAC and then put in mOutputFrames
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
                //TODO: inform AudioBroadcaster of this format.
                MediaFormat outFormat = mCodec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + outFormat);
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }


        }

        if(!mSeek){
            mLastFrame = mCurrentOutputID;
        } else {
            mSeek = false;
        }


        // TODO : notify clients of last frame?
        // TODO : implement fault tolerance
        mRunning = false;

        Log.d(LOG_TAG, "stopping...");

        releaseCodec();
        mDecoder.destroy();

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
//        Log.d(LOG_TAG , "Data size is: " + sampleSize + " mOldDataIndex: " + mDataIndex  + " , and currentFrame is " + mCurrentInputFrameID);

        int spaceLeft = dstBuf.capacity() - dstBuf.position();


        //If we've got enough space for the whole rest of the frame, then use the rest of the frame
        if(spaceLeft > (sampleSize - mDataIndex) ){
            if(mDataIndex != 0) {
                data = Arrays.copyOfRange(data, mDataIndex, sampleSize);
                mDataIndex = 0;
            }

            mInputFrames.remove(frame.getID());
//            Log.d(LOG_TAG , "1: Data is : " + data.length);
            mCurrentInputFrameID++;

        } else {
            //If not, then let's put what we can
            int endIndex = spaceLeft + mDataIndex;

            int startIndex = mDataIndex;
            //If the space left in the ByteBuffer is greater than the space left in the frame, then just put what's left
            if(endIndex >= data.length){
                endIndex = data.length;
                mCurrentInputFrameID++;
                mInputFrames.remove(frame.getID());
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

    private int mCount = 0;
    //Creates a frame out of PCM data and sends it to the AudioBroadcaster class.
    private void createFrame(byte[] data){
        AudioFrame frame = new AudioFrame(data, mCurrentOutputID , mSongID);
        mCurrentOutputID++;

        if(mCount == 100){
            mCount = 0;
            Log.d(LOG_TAG , "Frame #" + mCurrentOutputID + " created");
        }

        if(frame == null){
            Log.d(LOG_TAG , "AudioFrame is Null. It certainly should not be.");
        } else {
            synchronized (mOutputFrames) {
                mOutputFrames.put(frame.getID(), frame);
            }
        }

    }

    public void stop(){
        mStop = true;
        mSeek = true;
        long begin = System.currentTimeMillis();

        while (mRunning) {}

        Log.d(LOG_TAG, "Waiting for encode thread to finish , took " + (System.currentTimeMillis() - begin) + "ms");
    }


    public void seek(long seekTime){
        mDecoder.destroy();
        synchronized (mInputFrames){
            mInputFrames = new TreeMap<>();
        }
    }

    public void setSong(LocalSong song){
        mSong = song;
    }

    public void setInputFormat(MediaFormat format){
        mInputFormat = format;
    }

    public Map<Integer , AudioFrame> getFrames(){
        return mOutputFrames;
    }


    public void lastFrame(int currentID){
        mLastFrame = currentID;
    }

    public int getLastFrame(){
        return mLastFrame;
    }

    public void destroy(){

    }

    public boolean hasFrame(int ID){
        return mOutputFrames.containsKey(ID);
    }

    public AudioFrame getFrame(int ID){
        if(ID > mHighestFrameUsed) mHighestFrameUsed = ID;

        synchronized (mOutputFrames){
            AudioFrame frame = mOutputFrames.get(ID);

            if(frame == null)
                Log.d(LOG_TAG , "Frame is null! Error!");

            if(mRemoveFrames)
                mOutputFrames.remove(ID);

            return frame;
        }
    }

    public void setKeepFrames(boolean keepFrames){
        mKeepFrames = keepFrames;
    }
}
