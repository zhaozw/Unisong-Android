package io.unisong.android.audio.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.network.CONSTANTS;

/**
 * This is the abstract Decoder class which FileDecoder and SongDecoder inherit from.
 * It contains common code/variable declarations
 * Created by Ethan on 8/4/2015.
 */
public abstract class Decoder {

    protected static final int DEFAULT_FRAME_BUFFER_SIZE = 50;
    private static final String LOG_TAG = Decoder.class.getSimpleName();

    protected Map<Integer , AudioFrame> inputFrames, outputFrames;

    // The media codec object used to decode the files
    protected MediaCodec codec;

    // The formats for the input and output data
    protected MediaFormat inputFormat, outputFormat;

    protected Thread decodeThread;

    protected String mime;
    protected int sampleRate , channels, bitrate, frameBufferSize = DEFAULT_FRAME_BUFFER_SIZE
                    ,samples = 0, outputFrameID = 0, inputFrameID = 0;
    protected long presentationTimeUs = 0, duration = 0, timeAdjust, seekTime, playTime;
    // playTime is the variable in which the current frame's time is stored so we can restart on failure.

    protected boolean isRunning, stop;

    public Decoder(){
        // TODO : reorganize this class into sensical groupings of methods
        outputFrames = new HashMap<>();
        inputFrames = new HashMap<>();
    }


    public void start(){
        decodeThread = getDecodeThread();
        decodeThread.start();
    }

    public void start(long startTime){
        seekTime = startTime;
        timeAdjust = seekTime;
        start();
    }

    public AudioFrame getFrame(int ID){
        AudioFrame frame;
        synchronized (outputFrames){
            frame = outputFrames.get(ID);
            outputFrames.remove(ID);
        }
        return frame;
    }

    public boolean hasInputFrame(int ID){
        return inputFrames.containsKey(ID);
    }

    public boolean hasOutputFrame(int ID){
        return outputFrames.containsKey(ID);
    }

    public void addInputFrame(AudioFrame frame) {
        synchronized (inputFrames){
            inputFrames.put(frame.getID(), frame);
        }
    }

    protected abstract void configureCodec();

    public void seek(long seekTime) {
        stop = true;
        Log.d(LOG_TAG , "Stopping Thread");
        while (isRunning){
            synchronized (this){
                try{
                    this.wait(1);
                } catch (InterruptedException e){
                    Log.d(LOG_TAG , "Thread interrupted while waiting in seek()");
                }
            }
        }
        Log.d(LOG_TAG , "Thread stopped, starting new thread.");
        this.seekTime = seekTime;
        decodeThread = getDecodeThread();
        decodeThread.start();
    }


    /**
     *
     * @return decodeThread - the Thread that decoding will run on
     */
    protected Thread getDecodeThread(){
        return new Thread(() -> {
            try {
                isRunning = true;
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                configureCodec();
                decode();
                isRunning = false;
            } catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    protected abstract void decode();

    /**
     * Waits for a frame to be used, then
     */
    public void waitForFrameUse(){
        while(outputFrames.size() > frameBufferSize){
            if(stop){
                Log.d(LOG_TAG , "stop is true, exiting loop.");
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

    /**
     * A function that iterates through all available frames and determines whether we have
     * a frame at the specified time
     * @param time - the time to check for
     * @return hasAACFrame - the boolean telling us whether we have said frame
     */
    public boolean hasFrameAtTime(long time){
        // check if we have a frame with a playTime close to time
        synchronized (outputFrames){
            for (Map.Entry<Integer, AudioFrame> entry : outputFrames.entrySet()) {
                long diff = entry.getValue().getPlayTime() - time;

                // if so, return true
                if (Math.abs(diff) <= 22)
                    return true;
            }
        }

        // otherwise return false
        return false;
    }


    /**
     * A function that iterates through all available frames and returns the ID
     * of the frame at the specified time. Will return -1 if we do not have it
     * @param time - the time to check for
     * @return frameID - the ID of the frame if we have it, -1 otherwise
     */
    // TODO : rename?
    // TODO : delete all this debug code, it will significantly slow down this operation

    public int getFrameIDAtTime(long time){
        int lowestID = Integer.MAX_VALUE;
//        long lowestDiff = 999999999999999999l;
//        long highestPlayTime = -1;
        if(outputFrames.size() == 0)
            Log.d(LOG_TAG , "outputFrames is empty!");
        synchronized (outputFrames){
            for (Map.Entry<Integer, AudioFrame> entry : outputFrames.entrySet()) {
                long playTime = entry.getValue().getPlayTime();
                long diff = playTime - time;

//                if(diff < lowestDiff)
//                    lowestDiff = diff;

//                if(playTime > highestPlayTime)
//                    highestPlayTime = playTime;

                if(lowestID > entry.getKey())
                    lowestID = entry.getKey();

//                Log.d(LOG_TAG , "for frame #" + entry.getKey() + " playTime is : " + playTime);
                if (Math.abs(diff) <= 34)
                    return entry.getKey();
            }
        }

//        Log.d(LOG_TAG , "Frame not found at time " + time + "ms ! Lowest difference is : " + lowestDiff);
//        Log.d(LOG_TAG , "Highest frame playTime is: " + highestPlayTime + "ms");
        if(lowestID == Integer.MAX_VALUE)
            return 0;

        return lowestID;
    }



    public MediaFormat getOutputFormat(){
        return inputFormat;
    }

    public void setFrameBufferSize(int size){
        frameBufferSize = size;
    }

    protected void createPCMFrame(byte[] data){
        long bitsProcessed = samples * 8; // * 8000;
        long playTime = (bitsProcessed * 1000)   / CONSTANTS.PCM_BITRATE + timeAdjust;

        AudioFrame frame = new AudioFrame(data, outputFrameID, playTime);

        synchronized (outputFrames) {
            outputFrames.put(frame.getID(), frame);
        }

        if(outputFrameID % 100 == 0)
            Log.d(LOG_TAG, "Frame #" + outputFrameID + " created.");

        outputFrameID++;

        samples += data.length;

        waitForFrameUse();
    }

    public abstract void destroy();

    /**
     * Stops, releases, and sets to null the codec.
     */
    protected void releaseCodec(){
        if(codec != null) {
            codec.stop();
            codec.release();
            codec = null;
        }
    }

    protected void restartOnFailure(){
        if(!stop){
            // TODO : this is an interim fix. It'd be great to have a 'smarter' fault tolerance system
            Log.d(LOG_TAG, "We are stopping but stop is false! Restarting at current time");
            start(playTime);
        }
    }

}