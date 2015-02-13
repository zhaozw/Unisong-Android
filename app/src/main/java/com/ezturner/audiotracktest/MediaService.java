package com.ezturner.audiotracktest;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Ethan on 1/25/2015.
 */
public class MediaService extends Service{

    private IBinder mBinder = new MediaServiceBinder();

    private boolean mIsPlaying;
    private boolean mIsPaused;

    private static final String TEST_FILE_PATH = "/storage/emulated/0/music/05  My Chemical Romance - Welcome To The Black Parade.mp3";


    public MediaService(){

        mIsPaused = false;
        mIsPlaying = false;


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
