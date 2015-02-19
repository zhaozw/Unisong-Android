package com.ezturner.speakersync.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.ezturner.speakersync.MediaService;
import com.ezturner.speakersync.network.master.AudioBroadcaster;
import com.ezturner.speakersync.network.slave.AudioListener;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioFileReader {

    //The current file that is being read from.
    private File mCurrentFile;

    //The LAME decoder interface object
    private Decoder mDecoder;

    //The object that broadcasts audio frames
    private AudioBroadcaster mBroadcaster;

    //The object that handles the playback of audio data
    private AudioTrackManager mManager;

    //The listener that will sync the playback
    private AudioListener mListener;

    //The current ID of the audio frame
    private int mCurrentId;

    //Whether the decode thread should stop
    private boolean mDoStop;

    //Whether this is the first run
    private boolean mFirstRun;

    private long mSampleTime;


    public AudioFileReader(AudioBroadcaster broadcaster , AudioTrackManager manager){
        mBroadcaster = broadcaster;
        mCurrentId = 0;
        mDoStop = false;
        mSampleTime = -67;
    }

    public AudioFileReader(AudioListener listener , AudioTrackManager manager){
        mManager = manager;
        mCurrentId = 0;
        mDoStop = false;
        mSampleTime = -67;
    }

    public void readFile(String path) throws IOException{
        mCurrentFile = new File(path);
        mDecoder = new Decoder(mCurrentFile);
        try {
            mDecoder.initialize();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private Thread decode = new Thread(new Runnable()  {
        public void run() {
            try {
                startDecode();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    });

    private void startDecode() throws IOException{


        short[] data;

        //MediaPlayer mp = new MediaPlayer();
        //mp.setDataSource(this , Uri.fromFile(mCurrentFile));
        //int duration = mp.getDuration();

        boolean stopCode = false;
        boolean notDone = true;
        int counter = 0;
        while(!mDecoder.streamIsEmpty() || !stopCode){
            data = mDecoder.decodeOneFrame();
            if(data.length == 0){
                stopCode = true;
            } else {
                AudioFrame frame = null;//new AudioFrame(data , mCurrentId);
                mCurrentId++;

                if(mBroadcaster != null){
                    mBroadcaster.addPacket(frame);
                }

                mManager.addFrame(frame);
                //mAudioTrack.write(data, 0, data.length); Write the data to wherever

            }

            counter++;
            if(counter >= 550){
                System.gc();
                counter = 0;
            }

        }

        stopCode = false;

    }

    protected int inputBufIndex;
    protected int bufIndexCheck;
    protected int lastInputBufIndex;

    private void extractorDecode() throws IOException {
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        // extractor gets information about the stream
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource("/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3");
        } catch (Exception e) {
            return;
        }

        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);

        // the actual decoder
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        // get the sample rate to configure AudioTrack
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

        // create our AudioTrack instance
        mManager.createAudioTrack(sampleRate);

        // start playing, we will feed you later
        extractor.selectTrack(0);

        // start decoding
        final long kTimeOutUs = 10000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 50;


        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !mDoStop) {
            //Log.i(LOG_TAG, "loop ");
            noOutputCounter++;
            if (!sawInputEOS) {

                inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                bufIndexCheck++;
                // Log.d(LOG_TAG, " bufIndexCheck " + bufIndexCheck);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];


                    int sampleSize = extractor.readSampleData(dstBuf, 0 /* offset */);

                    long presentationTimeUs = 0;

                    if (sampleSize < 0) {
                        Log.d("ezturner", "saw input EOS.");
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        //In microseconds, the length of the sample
                        presentationTimeUs = extractor.getSampleTime();

                        if(mBroadcaster != null){
                            mBroadcaster.setFrameLength(presentationTimeUs);
                        }

                        Log.d("ezturner" , "Sample Time: " + presentationTimeUs);

                        if(mSampleTime == -67){
                            mSampleTime = presentationTimeUs;
                        } else if(mSampleTime != presentationTimeUs){
                            Log.d("ezturner" , "MAJOR ERROR: PRESENTATION TIME DIFFERENCE 1:" + mSampleTime + " 2 :" + presentationTimeUs );
                        }

                        byte[] data = new byte[sampleSize];

                        dstBuf.get(data);
                        int id = mCurrentId;
                        mCurrentId++;
                        AudioFrame frame;
                        if(mBroadcaster != null){
                            frame = new AudioFrame(data, id , mBroadcaster.getNextFrameWriteTime() , presentationTimeUs);
                            mBroadcaster.addPacket(frame);
                        } else {
                            frame = new AudioFrame(data, id , mListener.getNextFrameWriteTime() , presentationTimeUs);
                        }

                        mManager.addFrame(frame);
                    }
                    // can throw illegal state exception (???)

                    codec.queueInputBuffer(
                            inputBufIndex,
                            0 /* offset */,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);


                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                } else {
                    Log.e("ezturner", "inputBufIndex " + inputBufIndex);
                }
            }
        }
    }

}
