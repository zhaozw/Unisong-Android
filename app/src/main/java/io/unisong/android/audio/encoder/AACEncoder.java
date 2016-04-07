package io.unisong.android.audio.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.song.Song;

/**
 * This class handles the encoding of AAC data for transmission over
 * the network to client devices. An encoder is created for a song as a whole,
 * Created by Ethan on 4/27/2015.
 */
public class AACEncoder{
    private static final String LOG_TAG = AACEncoder.class.getSimpleName();

    // These are the default configurations of the AACData. DO NOT DEPEND ON THESE TO BE TRUE
    public static final int CHANNELS = 2;
    public static final int SAMPLE_RATE = 44100;
    public static final int BITRATE = CHANNELS * 64000;
    public static final String MIME = "audio/mp4a-latm";

    //The media codec object used to encode the files
    private MediaCodec codec;

    private AACEncoderThread encodeThread;

    private Song song;

    //The output and input frames
    private Map<Integer , AudioFrame> outputFrames;


    private String filePath;
    private MediaFormat inputFormat;
    private boolean seek = false;
    private int currentInputFrameID = 0;

    //The last frame, a signal that the input is ending
    private int lastFrame;

    //The highest frame # used.
    private int highestFrameUsed;
    private boolean removeFrames = true;

    public AACEncoder(Song song){
        highestFrameUsed = 0;
        this.song = song;

        outputFrames = new HashMap<>();

        lastFrame = -1;
        currentInputFrameID = 0;
    }

    /**
     * Tell the AACEncoder whether to remove frames after they are used.
     * Set to false by default.
     */
    public void setRemoveFrames(boolean remove){
        removeFrames = remove;
    }

    /**
     * Start the encoding
     * @param startTime
     * @param filePath
     */
    public void encode(long startTime, String filePath){
        this.filePath = filePath;

        // If we have a thread currently running, stop it
        if(encodeThread != null)
            encodeThread.stopEncoding();

        encodeThread = new AACEncoderThread(outputFrames, song.getID() , filePath);
        encodeThread.startEncode(startTime);
    }




    private void releaseCodec(){
        if(codec != null) {
            codec.stop();
            codec.release();
            codec = null;
        }
    }

    //The code to stop the decoding
    public void stopDecode(){
    }

    private int mCount = 0;


    public void stop(){
        if(encodeThread != null)
            encodeThread.stopEncoding();
    }


    /**
     * Seeks the AACEncoder, stopping the old thread(if it exists)
     * and starting another
     * @param seekTime
     */
    public void seek(long seekTime){
        // TODO : uncomment this as well.
        if(encodeThread != null)
            encodeThread.stopEncoding();

        encodeThread = new AACEncoderThread(outputFrames , song.getID(), filePath);
        encodeThread.startEncode(seekTime);
    }

    public void setInputFormat(MediaFormat format){
        inputFormat = format;
    }

    public Map<Integer , AudioFrame> getFrames(){
        return outputFrames;
    }


    public void lastFrame(int currentID){
        lastFrame = currentID;
    }

    public int getLastFrame(){
        return lastFrame;
    }

    public void destroy(){

    }

    public boolean hasFrame(int ID){
        return outputFrames.containsKey(ID);
    }

    public AudioFrame getFrame(int ID){
        if(ID > highestFrameUsed) highestFrameUsed = ID;

        synchronized (outputFrames){
            AudioFrame frame = outputFrames.get(ID);

            if(frame == null)
                Log.d(LOG_TAG , "Frame is null! Error!");

            if(removeFrames)
                outputFrames.remove(ID);

            return frame;
        }
    }
}
