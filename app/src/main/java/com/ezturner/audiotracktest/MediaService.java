package com.ezturner.audiotracktest;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.ezturner.audiotracktest.dds.ServiceEnvironmentImpl;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Ethan on 1/25/2015.
 */
public class MediaService extends Service{

    private final int MULTICAST_PORT = 1732;
    private final String MULTICAST_ADDRESS = "238.231.1.1";

    private IBinder mBinder = new MediaServiceBinder();

    private File mCurrentFile;
    private AudioTrack mAudioTrack;

    private boolean mIsPlaying;
    private boolean mIsPaused;
    private Decoder mDecoder;

    private String filepath = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";


    public MediaService(){

        mIsPaused = false;
        mIsPlaying = false;

        mCurrentFile = new File(filepath);

        mDecoder = new Decoder(mCurrentFile);
        int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);


        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        Log.d("ezturner" , "AudioTrack made? "  + mAudioTrack.toString());
        try {
            mDecoder.initialize();
        } catch(IOException e){
            e.printStackTrace();
        }


    }

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void processMP3() {
        final Runnable processer = new Runnable() {
            public void run() {
                try {
                    scheduledDecode();
                } catch (Exception e){
                    e.printStackTrace();
                }
            };
        };
        //mDecodeHandler = scheduler.scheduleAtFixedRate( processer, 0, 2, TimeUnit.MILLISECONDS);
    }
/*
    private void extractMP3Data() throws IOException{

        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(filepath);
        int numTracks = extractor.getTrackCount();

        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        extractor.selectTrack(0);

        ByteBuffer inputBuffer = ByteBuffer.allocate(9203712);
        while (extractor.readSampleData(inputBuffer, 0) >= 0) {

            extractor.advance();
        }

        extractor.release();
        extractor = null;

    }*/
    private boolean stopCode = false;
    private boolean threadRunning = false;

    public void stopCode(){
        stopCode = true;
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
        mAudioTrack.play();

        //MediaPlayer mp = new MediaPlayer();
        //mp.setDataSource(this , Uri.fromFile(mCurrentFile));
        //int duration = mp.getDuration();

        boolean notDone = true;
        int counter = 0;
        while(!mDecoder.streamIsEmpty() || !stopCode){
            data = mDecoder.decodeOneFrame();
            if(data.length == 0){
                stopCode = true;
                Log.d("ezturner" , "Done!");
            } else {
                mAudioTrack.write(data, 0, data.length);
            }
            Log.d("ezturner" , "run");
            counter++;
            if(counter >= 550){
                System.gc();
                counter = 0;
            }


        }

        stopCode = false;

    }

    private void scheduledDecode(){
        /*
        short[] buffer;
        Header head = mBitstream.readFrame();

        //SampleBuffer sampleBuffer = (SampleBuffer) mDecoder.decodeFrame(head, mBitstream);

        //buffer = sampleBuffer.getBuffer();

        for (int i = 0; i < buffer.length / mDivider; i++) {
            mOutStream.write(buffer[i] & 0xff);
            mOutStream.write((buffer[i] >> 8) & 0xff);
        }

        mBitstream.closeFrame();


        byte[] byteArray = mOutStream.toByteArray();
            mAudioTrack.write(byteArray, 0, byteArray.length);
        */
    }

    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class MediaServiceBinder extends Binder {
        /**
         * Returns the instance of this service for a client to make method calls on it.
         * @return the instance of this service.
         */
        public MediaService getService() {
            return MediaService.this;
        }

    }


    public void togglePlay(){
        if(!mIsPlaying && !mIsPaused){
            play();
        } else if(mIsPlaying && !mIsPaused){
            pause();
        } else if(mIsPaused){
            resume();
        }
    }

    private void play(){

        mServiceEnv.publish();

        /* if(!mIsPlaying) {
            decode.start();
        } else {
            mAudioTrack.play();
        }
        mIsPlaying = true;

        processMP3();

        */
    }

    private void pause(){
        mIsPaused = true;


       // mAudioTrack.pause();
    }

    private void resume(){
        mIsPaused = false;

        //mAudioTrack.play();
    }


}
