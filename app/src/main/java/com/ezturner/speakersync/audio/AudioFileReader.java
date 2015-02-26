package com.ezturner.speakersync.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;

import com.ezturner.speakersync.network.master.AudioBroadcaster;
import com.ezturner.speakersync.network.slave.AudioListener;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioFileReader {

    private static final String LOG_TAG = "AudioFileReader";

    private static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";

    //The current file that is being read from.
    private File mCurrentFile;

    //The object that broadcasts audio frames
    private AudioBroadcaster mBroadcaster;

    //The object that handles the playback of audio data
    private AudioTrackManager mManager;

    //The listener that will sync the playback
    private AudioListener mListener;

    //The current ID of the audio frame
    private int mCurrentId;

    //Whether this is the first run
    private boolean mFirstRun;

    private long mSampleTime;

    //The MediaExtractor that will handle the data extraction
    private MediaExtractor mExtractor;

    //The event handler for the audio reading
    private AudioFileReaderEvents mEvents;

    //The media codec object used to decode the files
    private MediaCodec mCodec;

    private int mSourceRawResId = -1;
    private Context mContext;
    private boolean mStop = false;

    Handler handler = new Handler();

    String mime = null;
    int sampleRate = 0, channels = 0, bitrate = 0;
    long presentationTimeUs = 0, duration = 0;

    private PlayerStates mState;

    private Thread mDecodeThread;

    //The constructor for broadcasting
    public AudioFileReader(AudioBroadcaster broadcaster , AudioTrackManager manager){
        this(manager);
        mBroadcaster = broadcaster;
    }

    //The constructor for listening
    public AudioFileReader(AudioListener listener , AudioTrackManager manager){
        this(manager);
        mListener = listener;
    }

    public AudioFileReader(AudioTrackManager manager){
        mManager = manager;
        mStop = false;
        mSampleTime = -67;
        mCurrentId = 0;
        mEvents = new AudioFileReaderEvents();
        mState = new PlayerStates();
    }

    public void readFile(String path) throws IOException{
        mCurrentFile = new File(path);
        mDecodeThread = getDecode();
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

    protected int inputBufIndex;
    protected int bufIndexCheck;
    protected int lastInputBufIndex;

    public void decode() throws IOException {
        long startTime = System.currentTimeMillis();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        // mExtractor gets information about the stream
        mExtractor = new MediaExtractor();
        // try to set the source, this might fail
        try {
            //TODO: Set the file path to dynamic
            mExtractor.setDataSource(mCurrentFile.getPath());


            /* This is the code for using internal app resources
            if (mSourceRawResId != -1) {
                AssetFileDescriptor fd = mContext.getResources().openRawResourceFd(mSourceRawResId);
                mExtractor.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength());
                fd.close();
            }*/
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
            Log.e(LOG_TAG, "Reading format parameters exception: "+e.getMessage());
            e.printStackTrace();
            // don't exit, tolerate this error, we'll fail later if this is critical
        }
        Log.d(LOG_TAG, "Track info: mime:" + mime + " sampleRate:" + sampleRate + " channels:" + channels + " bitrate:" + bitrate + " duration:" + duration);

        // check we have audio content we know
        if (format == null || !mime.startsWith("audio/")) {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onError();  } });
            return;
        }
        // create the actual decoder, using the mime to select
        try {
            mCodec = MediaCodec.createDecoderByType(mime);
        } catch(IOException e){
            e.printStackTrace();
        }
        // check we have a valid codec instance
        if (mCodec == null) {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onError();  } });
            return;
        }

        //state.set(PlayerStates.READY_TO_PLAY);
        if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onStart(mime, sampleRate, channels, duration);  } });

        mCodec.configure(format, null, null, 0);
        mCodec.start();
        ByteBuffer[] codecInputBuffers  = mCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();

        // configure AudioTrack
        int channelConfiguration = channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int minSize = AudioTrack.getMinBufferSize( sampleRate, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT);
        mManager.setAudioTrack(new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfiguration,
                AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM));

        // start playing, we will feed the AudioTrack later
        //audioTrack.play();
        mExtractor.selectTrack(0);

        // start decoding
        final long kTimeOutUs = 1000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 10;

        mState.set(PlayerStates.PLAYING);
        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !mStop) {

            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            if (!sawInputEOS) {
                int inputBufIndex = mCodec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    int sampleSize = mExtractor.readSampleData(dstBuf, 0);
                    if (sampleSize < 0) {
                        Log.d(LOG_TAG, "saw input EOS. Stopping playback");
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = mExtractor.getSampleTime();
                        Log.d(LOG_TAG , "Presentation Time : " + presentationTimeUs);
                        final int percent =  (duration == 0)? 0 : (int) (100 * presentationTimeUs / duration);
                        //if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onPlayUpdate(percent, presentationTimeUs / 1000, duration / 1000);  } });
                    }

                    mCodec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    if (!sawInputEOS) mExtractor.advance();

                } else {
                    Log.e(LOG_TAG, "inputBufIndex " +inputBufIndex);
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
                    createFrame(chunk);
                	/*if(this.state.get() != PlayerStates.PLAYING) {
                		if (events != null) handler.post(new Runnable() { @Override public void run() { events.onPlay();  } });
            			state.set(PlayerStates.PLAYING);
                	}*/

                }
                mCodec.releaseOutputBuffer(outputBufIndex, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = mCodec.getOutputBuffers();
                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = mCodec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + oformat);
            } else {
                Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
        }

        Log.d(LOG_TAG, "stopping...");

        if(mCodec != null) {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }



        // clear source and the other globals
        //sourcePath = null;
        mSourceRawResId = -1;
        duration = 0;
        mime = null;
        sampleRate = 0; channels = 0; bitrate = 0;
        presentationTimeUs = 0; duration = 0;


        mState.set(PlayerStates.STOPPED);
        mStop = true;

        if(noOutputCounter >= noOutputCounterLimit) {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onError();  } });
        } else {
            if (mEvents != null) handler.post(new Runnable() { @Override public void run() { mEvents.onStop();  } });
        }

        long finishTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Total time taken : " + (finishTime - startTime) / 1000 + " seconds");
        //mManager.startPlaying();
    }


    private void createFrame(byte[] data){
        AudioFrame frame  = new AudioFrame(data , mCurrentId );;
        if(mBroadcaster != null){
            mBroadcaster.addPacket(frame);
        }
        mManager.addFrame(frame);

        mCurrentId++;
    }



}
