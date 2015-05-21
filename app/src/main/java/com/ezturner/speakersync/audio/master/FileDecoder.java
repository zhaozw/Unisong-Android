package com.ezturner.speakersync.audio.master;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.ezturner.speakersync.audio.ReaderToReaderBridge;
import com.ezturner.speakersync.network.slave.NetworkInputStream;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ezturner on 5/20/2015.
 */
public class FileDecoder {

    private static final String LOG_TAG = FileDecoder.class.getSimpleName();

    private boolean mStop = false;

    //The file that is being read from.
    private File mCurrentFile;

    //The MediaExtractor that will handle the data extraction
    private MediaExtractor mExtractor;

    private long mSampleTime;
    private boolean mHasSampleTime;

    private Thread mDecodeThread;

    private boolean mFirstRun;

    private boolean mRunning;
    private String mime = null;
    private int sampleRate = 0, channels = 0, bitrate = 0;
    private long presentationTimeUs = 0, duration = 0;

    //The media codec object used to decode the files
    private MediaCodec mCodec;

    private AudioFileReader mReader;

    public FileDecoder(String path, AudioFileReader parent , long seekTime){

        mReader = parent;
        //Set the variables
        mHasSampleTime = false;
        mSeekTime = seekTime;
        mRunning = false;

        //Create the file and start the Thread.
        mCurrentFile = new File(path);
        mDecodeThread = getDecode();
        mDecodeThread.start();
    }


    private Thread getDecode(){
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


    private NetworkInputStream mInputStream;

    private long mSize;
    //A boolean telling us when the first Output format is changed, so that we can start the AAC Encoder
    private boolean mFirstOutputChange = true;
    private long mSeekTime;
    private Long mPlayTime = 0l;

    private void decode() throws IOException {
        mInputStream = new NetworkInputStream();

        long startTime = System.currentTimeMillis();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        mFirstRun = true;

        long startSampleTime = -1;
        mRunning = true;
        // mExtractor gets information about the stream
        mExtractor = new MediaExtractor();
        // try to set the source, this might fail
        try {
            mExtractor.setDataSource(mCurrentFile.getPath());
            mExtractor.seekTo((mSeekTime - 50) * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);


        } catch (Exception e) {
            e.printStackTrace();
            //TODO : Handle this exception
            return;
        }

        // Read track header
        MediaFormat format = null;
        try {
            format = mExtractor.getTrackFormat(0);
            mime = format.getString(MediaFormat.KEY_MIME);
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            // if duration is 0, we are probably playing a live stream
            duration = format.getLong(MediaFormat.KEY_DURATION);
            bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Reading format parameters exception: " + e.getMessage());
            e.printStackTrace();
            // don't exit, tolerate this error, we'll fail later if this is critical
            //TODO: figure out what to do with this error
        }
        Log.d(LOG_TAG, "Track info: mime:" + mime + " sampleRate:" + sampleRate + " channels:" + channels + " bitrate:" + bitrate + " duration:" + duration);


        // create the actual decoder, using the mime to select
        try {
            mCodec = MediaCodec.createDecoderByType(mime);
        } catch(IOException e){
            e.printStackTrace();
        }

        mCodec.configure(format, null, null, 0);
        mCodec.start();
        ByteBuffer[] codecInputBuffers  = mCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();


        mReader.createEncoder();
        mExtractor.selectTrack(0);

        // start decoding
        final long kTimeOutUs = 1000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        Log.d(LOG_TAG , "Info size is : "  + info.size);
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 10;

        mStop = false;

        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !mStop) {


            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            if (!sawInputEOS) {
                int inputBufIndex = mCodec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0){
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    int sampleSize = mExtractor.readSampleData(dstBuf, 0);
                    if (sampleSize < 0){
                        Log.d(LOG_TAG, "saw input EOS. Stopping playback");
                        mReader.lastPacket();
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {

                        mPlayTime = mExtractor.getSampleTime() / 1000;
//                        Log.d(LOG_TAG , "PlayTime is : " + mPlayTime);

                        presentationTimeUs = mExtractor.getSampleTime();
                        final int percent =  (duration == 0)? 0 : (int) (100 * presentationTimeUs / duration);
                        //if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onPlayUpdate(percent, presentationTimeUs / 1000, duration / 1000);  } });
                    }



                    mCodec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (!sawInputEOS) mExtractor.advance();

                } else {
                    //TODO: Investigate and reenable
                    //Log.e(LOG_TAG, "inputBufIndex " +inputBufIndex);
                }
            } // !sawInputEOS

            // decode to PCM and push it to the AudioTrack player
            int res = mCodec.dequeueOutputBuffer(info, kTimeOutUs);

            if (res >= 0) {
                if (info.size > 0)  noOutputCounter = 0;

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if(chunk.length > 0){

                    mSize += chunk.length;

                    if(!mStop)  mReader.createPCMFrame(chunk);
                	/*if(this.state.get() != PlayerStates.PLAYING) {
                		if (events != null) handler.post(new Runnable() { @Override public void run() { events.onPlay();  } });
            			state.set(PlayerStates.PLAYING);
                	}*/

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
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                codecOutputBuffers = mCodec.getOutputBuffers();
                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                MediaFormat oformat = mCodec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + oformat);
                if (mFirstOutputChange){
                    MediaFormat outFormat = mCodec.getOutputFormat();
                    int outputChannels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int outputSampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);

                    mFirstOutputChange = false;
                    Log.d(LOG_TAG, "Starting AAC Encoder now");


                    mReader.setAudioInfo(outputChannels);
                    Log.d(LOG_TAG, "Output channels are : " + outputChannels);
                    mReader.createAudioTrack(outputSampleRate , outputChannels);

                    //TODO: Ensure that the output format is always 2 channels and 44100 sample rate
                    mReader.setEncoderFormat(format);
                }
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }

            mFirstRun = false;
        }


        mRunning = false;
        Log.d(LOG_TAG, "stopping...");

        long finishTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Total time taken : " + (finishTime - startTime) / 1000 + " seconds");
        Log.d(LOG_TAG , "Size is : " + mSize);

        releaseCodec();
    }

    private void releaseCodec(){
        if(mCodec != null) {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }

    public void stop(){
        mStop = true;

        while(mRunning){}

        mExtractor = null;
        mCurrentFile = null;

    }

    public boolean isRunning(){
        return mRunning;
    }
}
