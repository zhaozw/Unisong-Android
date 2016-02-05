package io.unisong.android.audio.decoder;

import android.media.MediaCodec;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.song.SongFormat;

/**
 * A decoder class that decodes a single song.
 * Created by ezturner on 5/6/2015.
 */
public class UnisongDecoder extends Decoder {

    private final String LOG_TAG = UnisongDecoder.class.getSimpleName();

    private boolean isRunning = false;

    //The index for the read datas.
    private int dataIndex = 0;

    //The media codec object used to decode the files
    private MediaCodec codec;



    private int frameBufferSize;
    private SongFormat songFormat;



    public UnisongDecoder(SongFormat format){
        super();
        songFormat = format;
        inputFormat = songFormat.getMediaFormat();
        frameBufferSize = 50;
        outputFrameID = 0;
        mime = format.getMime();
        bitrate = format.getBitrate();
        sampleRate = format.getSampleRate();
        isRunning = false;
    }


    @Override
    public void start(long startTime){
        Log.d(LOG_TAG , "UnisongDecoder decode started at time: " + startTime);
        double frameDuration = 1000.0 * 1024.0 / songFormat.getSampleRate();
        inputFrameID = (int) (startTime / frameDuration);
        outputFrameID = 0;
        decodeThread = getDecodeThread();
        decodeThread.start();
    }

    @Override
    protected void configureCodec(){
        try{
            //TODO: see if this works on all devices.
            codec = MediaCodec.createDecoderByType(mime);
        } catch(IOException e){
            Log.d(LOG_TAG , "Codec creation failed.");
            e.printStackTrace();
            return;
        }

        codec.configure(inputFormat, null, null, 0);
    }

    @Override
    protected void decode(){

        long startTime = System.currentTimeMillis();

        codec.start();
        ByteBuffer[] codecInputBuffers  = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

        // start decoding
        final long kTimeOutUs = 1000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        Log.d(LOG_TAG , "Info size is : "  + info.size);
        //TODO: set sawInputEOS when we see the input EOS
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 10;

        isRunning = true;

        boolean firstOutputChange = true;
        long lastPlayTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Starting decode loop.");
        //TODO: deal with no data/end of stream
        while(!stop){

            waitForFrame(info.size);

            if(stop)   break;


            AudioFrame frame;
            synchronized (inputFrames){
                frame = inputFrames.get(inputFrameID);
            }

            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            if (!sawInputEOS){
                int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0){
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    dstBuf.clear();

                    int sampleSize = setData(frame , dstBuf);

                    codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                }
            }

            //TODO : check for illegal state exception?
            //TODO: This throws an illegal state exception pretty often, try to fix it.
            // encode to AAC and then put in inputFrames
            int outputBufIndex = 0;
            try {
                 outputBufIndex = codec.dequeueOutputBuffer(info, kTimeOutUs);
            } catch (IllegalStateException e){
                e.printStackTrace();
            }

            if (outputBufIndex >= 0){
                if (info.size > 0)  noOutputCounter = 0;

                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if(chunk.length > 0 && !stop){
                    createPCMFrame(chunk);
                }

                try {
                    codec.releaseOutputBuffer(outputBufIndex, false);
                } catch(IllegalStateException e){
                    e.printStackTrace();
                }
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.d(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                codecOutputBuffers = codec.getOutputBuffers();
                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                outputFormat = codec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + outputFormat);
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
        }

        Log.d(LOG_TAG, "stopping...");

        releaseCodec();
        isRunning = false;
        // clear source and the other globals
        //sourcePath = null;

        long finishTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Total time taken : " + (finishTime - startTime) / 1000 + " seconds");

        restartOnFailure();
    }


    //Figures out how much data to put in the ByteBuffer dstBuf
    //TODO: Make this more efficient by getting rid of the byte array assignments and check if it makes a difference
    private int setData(AudioFrame frame , ByteBuffer dstBuf){
        byte[] data = frame.getData();
        int sampleSize = data.length;

        int spaceLeft = dstBuf.capacity() - dstBuf.position();


        //If we've got enough space for the whole rest of the frame, then use the rest of the frame
        if(spaceLeft > (sampleSize - dataIndex)){
            if(dataIndex != 0) {
                data = Arrays.copyOfRange(data, dataIndex, sampleSize);
                dataIndex = 0;
            }
            inputFrameID++;

        } else {
            //If not, then let's put what we can
            int endIndex = spaceLeft + dataIndex;

            int startIndex = dataIndex;
            //If the space left in the ByteBuffer is greater than the space left in the frame, then just put what's left
            if(endIndex >= data.length){
                endIndex = data.length;
                inputFrameID++;
                dataIndex = 0;
            } else {
                dataIndex = endIndex;
            }

            data = Arrays.copyOfRange(data, startIndex , endIndex);
        }

        dstBuf.put(data);

        return data.length;
    }

    public void destroy(){
        stop = true;
        while(isRunning){}
        decodeThread = null;
        outputFrames = null;
    }


    private void waitForFrame(int size){
        boolean firstWait = true;
        //TODO: Rewrite this to feed blank AAC frames instead of creating an empty PCM one
        while(!inputFrames.containsKey(inputFrameID)){

            if(firstWait) {
                Log.d(LOG_TAG, "Waiting for frame #" + inputFrameID);
                firstWait = false;
            }

            if(stop){
                break;
            }

            try {
                synchronized (this){
                    this.wait(2);
                }
            } catch (InterruptedException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "Interrupted in waitForFrame()");
            }

        }
    }

    public Map<Integer, AudioFrame> getInputFrames(){
        return inputFrames;
    }

    public void setInputFrames(Map<Integer, AudioFrame> inputFrames){
        this.inputFrames = inputFrames;
    }

}