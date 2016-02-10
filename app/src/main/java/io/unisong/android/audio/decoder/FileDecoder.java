package io.unisong.android.audio.decoder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import io.unisong.android.audio.encoder.AACEncoder;
import io.unisong.android.audio.song.Song;

/**
 * The implementation of Decoder that decodes a local file.
 * Created by ezturner on 5/20/2015.
 */
public class FileDecoder extends Decoder{

    private static final String LOG_TAG = FileDecoder.class.getSimpleName();

    //The file that is being read from.
    private File inputFile;


    private Song song;
    private MediaExtractor extractor;
    private AACEncoder mEncoder;

    public FileDecoder(String path){
        super();
        //Create the file and start the Thread.
        inputFile = new File(path);
    }

    public void registerSong(Song song){
        this.song = song;
    }

    public void setEncoder(AACEncoder encoder){
        mEncoder = encoder;
    }

    @Override
    protected void configureCodec(){

        // extractor gets information about the stream
        extractor = new MediaExtractor();
        // try to set the source, this might fail
        try {
            extractor.setDataSource(inputFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            //TODO : Handle this exception
            return;
        }

        Log.d(LOG_TAG, inputFile.getPath());
        // Read track header
        try {
            inputFormat = extractor.getTrackFormat(0);
            mime = inputFormat.getString(MediaFormat.KEY_MIME);
            sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            // if duration is 0, we are probably playing a live stream
            duration = inputFormat.getLong(MediaFormat.KEY_DURATION);
            if(inputFormat.containsKey(MediaFormat.KEY_BIT_RATE))
                bitrate = inputFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Reading format parameters exception: " + e.getMessage());
            e.printStackTrace();
            // don't exit, tolerate this error, we'll fail later if this is critical
        }
        Log.d(LOG_TAG, "Track info: mime:" + mime + " sampleRate:" + sampleRate + " channels:" + channels + " bitrate:" + bitrate + " duration:" + duration);


        // create the actual decoder, using the mime to select
        try {
            codec = MediaCodec.createDecoderByType(mime);
        } catch(IOException e){
            e.printStackTrace();
        }

        codec.configure(inputFormat, null, null, 0);
    }


    protected void decode(){
        long startTime = System.currentTimeMillis();

        Long playTime = 0l;
        isRunning = true;

        codec.start();
        ByteBuffer[] codecInputBuffers  = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

        // selectTrack must be above seekTo
        extractor.selectTrack(0);

        // seek if we are seeking/starting late
        if(seekTime != 0) {
            extractor.seekTo((seekTime - 50) * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            Log.d(LOG_TAG, "extractor sample time is :" + extractor.getSampleTime() + " from SeekTime : " + seekTime);
        }

        // start decoding
        final long kTimeOutUs = 1000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//        Log.d(LOG_TAG , "Info size is : "  + info.size);
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 25;

        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !stop) {

            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            if (!sawInputEOS) {
                int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0){
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    int sampleSize = extractor.readSampleData(dstBuf, 0);
                    if (sampleSize < 0){
                        Log.d(LOG_TAG, "saw input EOS. Stopping playback");
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {

                        playTime = extractor.getSampleTime() / 1000;
//                        if(seekTime == 100000) Log.d(LOG_TAG , "PlayTime is : " + playTime);

                        presentationTimeUs = extractor.getSampleTime();
                        final int percent =  (duration == 0)? 0 : (int) (100 * presentationTimeUs / duration);
//                        if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onPlayUpdate(percent, presentationTimeUs / 1000, duration / 1000);  } });
                    }



                    codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (!sawInputEOS) extractor.advance();

                } else {
                    //TODO: Investigate and reenable
                    //Log.e(LOG_TAG, "inputBufIndex " +inputBufIndex);
                }
            } // !sawInputEOS

            // decode to PCM and push it to the AudioTrack player
            int res = codec.dequeueOutputBuffer(info, kTimeOutUs);

            if (res >= 0) {
                if (info.size > 0)  noOutputCounter = 0;

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if(chunk.length > 0){

                    if(!stop)  createPCMFrame(chunk);

                }

                try {
                    codec.releaseOutputBuffer(outputBufIndex, false);
                } catch(IllegalStateException e){
                    e.printStackTrace();
                }

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.d(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                    stop = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                codecOutputBuffers = codec.getOutputBuffers();
                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                outputFormat = codec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + outputFormat);
                if(mEncoder != null)
                    mEncoder.setInputFormat(outputFormat);
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }

            waitForFrameUse();
        }


        Log.d(LOG_TAG, "stopping...");

        long finishTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Total time taken : " + (finishTime - startTime) / 1000 + " seconds");
        Log.d(LOG_TAG , "Bytes processed : " + samples);

        Log.d(LOG_TAG , "stop : "  + stop);
        Log.d(LOG_TAG , "No Output Index: " + noOutputCounter);



        try {
            releaseCodec();
        } catch (IllegalStateException e){
            e.printStackTrace();
        }

        if(sawOutputEOS){
            // TODO : set this to 0 instead of 5
            while(outputFrames.size() > 0){
                try {
                    synchronized (this) {
                        this.wait(5);
                    }
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            notifyDone();
            return;
        }
        restartOnFailure();
    }

    public void destroy(){

        stop = true;

        while(isRunning){

        }
        outputFrames = new HashMap<>();
        inputFrames = new HashMap<>();

        codec = null;
        extractor = null;
        song = null;
    }

    private void notifyDone(){
        if(song != null) {
            song.notifyDone();
        }
    }


}
