package io.unisong.android.audio.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.decoder.FileDecoder;
import io.unisong.android.audio.song.LocalSong;

/**
 * This class represents a single Thread running in the AACEncoder class
 * This has its own class to prevent issues with starting and stopping threads.
 * Created by ezturner on 2/1/2016.
 */
public class AACEncoderThread extends Thread {

    private static final String LOG_TAG = AACEncoderThread.class.getSimpleName();

    private boolean running = false , waiting = false;
    private int dataIndex = 0;

    private long startTime;
    private Map<Integer, AudioFrame> inputFrames;
    private Map<Integer, AudioFrame> outputFrames;

    private int currentInputID, currentOutputID;
    private int frameBufferSize = 50, songID;

    private String filePath;
    private LocalSong song;


    long presentationTimeUs = 0;

    private FileDecoder decoder;
    private MediaCodec codec;

    public AACEncoderThread(Map<Integer, AudioFrame> outputFrames,
                            int songID , String filePath){
        this.outputFrames = outputFrames;
        this.songID = songID;
        this.filePath = filePath;
        inputFrames = new HashMap<>();
    }

    public void setFrameBufferSize(int size){
        frameBufferSize = size;
    }

    public void startEncode(){
        currentInputID = 0;
        decoder = new FileDecoder(filePath);
        decoder.start(startTime);
        Log.d(LOG_TAG , "Starting for path : " + filePath);
        this.start();
    }

    public void startEncode(long time){
        startTime = time;
        currentOutputID = (int) (startTime / (1024000.0 / 44100.0));
        startEncode();
    }

    @Override
    public void run(){
        running = true;
        encode();
        running = false;
    }

    private void encode(){
        // TODO : get rid of frames that have been played.
        long startTime = System.currentTimeMillis();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        Log.d(LOG_TAG, "Creating codec : OMX.google.aac.encoder");
        // create the actual decoder, using the mime to select
        try{
            //TODO: see if this works on all devices.
            codec = MediaCodec.createEncoderByType(AACEncoder.MIME);
        } catch(IOException e){
            e.printStackTrace();
        }
        // check we have a valid codec instance
        if (codec == null){
            Log.d(LOG_TAG , "codec is null ):");
            return;
        }


        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, AACEncoder.MIME);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC); //fixed version
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, AACEncoder.SAMPLE_RATE);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64000 * AACEncoder.CHANNELS);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, AACEncoder.CHANNELS);

        //song.setFormat(format);

        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
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

        int largestSize = 0;


        //TODO: deal with no data/end of stream
        while(running){


            if(!inputFrames.containsKey(currentInputID)) {
                boolean firstWait = true;
                while (!decoder.hasOutputFrame(currentInputID)) {
                    if(firstWait) {
                        Log.d(LOG_TAG , "waiting for frame #" + currentInputID);
                        firstWait = false;
                    }

                    if (!running) {
                        break;
                    }

                    try {
                        waiting = true;
                        synchronized (this) {
                            this.wait(10);
                        }
                        waiting = false;
                    } catch (InterruptedException e) {
                        Log.d(LOG_TAG, "Waiting interrupted");
                        waiting = false;
                    }


                }

                inputFrames.put(currentInputID, decoder.getFrame(currentInputID));
            }

            if(!inputFrames.containsKey(currentInputID))
                Log.d(LOG_TAG , "Does not contain key");

            while(outputFrames.size() >= frameBufferSize){
                try{
                    synchronized (this){
                        this.wait(10);
                    }
                } catch (InterruptedException e){

                }

            }




            AudioFrame frame = inputFrames.get(currentInputID);

            // TODO : replace this when we've got a better idea what's going on
            if(frame == null)
                continue;

//            Log.d(LOG_TAG , frame.toString());
            long playTime = -1;
            long length = -1;

            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
            if (inputBufIndex >= 0) {
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                dstBuf.clear();

                int sampleSize = setData(frame, dstBuf);

                codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs,
                        sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

            }


            // encode to AAC and then put in outputFrames
            int outputBufIndex = codec.dequeueOutputBuffer(info, kTimeOutUs);


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
                //TODO: inform AudioBroadcaster of this format.
                MediaFormat outFormat = codec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + outFormat);
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }


        }

        Log.d(LOG_TAG, "stopping...");

        decoder.destroy();

        long finishTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Total time taken : " + (finishTime - startTime) / 1000.0 + " seconds");
    }

    /**
     * This method will grab data from the AudioFrame, and place it in the dstBuf
     * @param frame - the frame to retrieve data from
     * @param dstBuf - the ByteBuffer to place data into
     * @return
     */
    //TODO: Make this more efficient by getting rid of the byte array assignments and check if it makes a difference
    private int setData(AudioFrame frame , ByteBuffer dstBuf){

        byte[] data = frame.getData();
        int sampleSize = data.length;
//        Log.d(LOG_TAG , "Data size is: " + sampleSize + " mOldDataIndex: " + dataIndex  + " , and currentFrame is " + currentInputID);

        int spaceLeft = dstBuf.capacity() - dstBuf.position();


        //If we've got enough space for the whole rest of the frame, then use the rest of the frame
        if(spaceLeft > (sampleSize - dataIndex) ){
            if(dataIndex != 0) {
                data = Arrays.copyOfRange(data, dataIndex, sampleSize);
                dataIndex = 0;
            }

            inputFrames.remove(frame.getID());
//            Log.d(LOG_TAG , "1: Data is : " + data.length);
            currentInputID++;

        } else {
            //If not, then let's put what we can
            int endIndex = spaceLeft + dataIndex;

            int startIndex = dataIndex;
            //If the space left in the ByteBuffer is greater than the space left in the frame, then just put what's left
            if(endIndex >= data.length){
                endIndex = data.length;
                currentInputID++;
                inputFrames.remove(frame.getID());
                dataIndex = 0;
            } else {
                dataIndex = endIndex;
            }

            data = Arrays.copyOfRange(data, startIndex , endIndex);
//            Log.d(LOG_TAG , "2: Data is : " + data.length);
        }

        dstBuf.put(data);

        return data.length;
    }

    public void stopEncoding(){
        running = false;

        if(!waiting)
            return;

        synchronized (this) {
            this.interrupt();
        }
    }

    //Creates a frame out of PCM data and adds it to the outputFrames
    private void createFrame(byte[] data){
        AudioFrame frame = new AudioFrame(data, currentOutputID, songID);
        currentOutputID++;

        if(currentOutputID % 100 == 0)
            Log.d(LOG_TAG , "Frame #" + currentOutputID + " created");


        if(frame == null){
            Log.d(LOG_TAG , "AudioFrame is Null. It certainly should not be.");
        } else {
            synchronized (outputFrames) {
                outputFrames.put(frame.getID(), frame);
            }
        }

    }

}
