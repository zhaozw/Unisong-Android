package com.ezturner.speakersync.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

import com.ezturner.speakersync.network.TimeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioTrackManager {

    private String LOG_TAG = "AudioTrackManager";

    //The AudioTrack used for playback
    private AudioTrack mAudioTrack;

    //A map of all of the frames and their IDs
    private Map<Integer , AudioFrame> mFrames;

    //the last frame ID that has been played
    private int mFrameToPlay;

    //boolean telling the Write thread to continue or not
    private boolean mIsPlaying;

    //The thread that the writing will go on
    private Thread mWriteThread;

    //The current last frame
    private int mLastFrameID;

    //The handler for scheduling resyncs and the song start
    private Handler mHandler;

    //The time that the current or next song will start at
    private long mSongStartTime;

    //The last frame added's ID
    private int mLastAddedFrameID;

    //The class that handles all of the time calculations
    private TimeManager mTimeManager;

    //The boolean telling us if we have received a seek command
    private boolean mSeek = false;

    private long mLastFrameTime;

    //TODO: Make AudioTrack configuration dynamic with the details of the file
    public AudioTrackManager(TimeManager manager){
        mFrames = new TreeMap<Integer , AudioFrame>();

        mThreadRunning = false;
        mIsPlaying = false;

        mFrameToPlay = 0;

        mTimeManager = manager;

        mLastFrameID = -89;
        mLastFrameTime = 0;

        mHandler = new Handler();
    }

    private boolean mStartSongRunning = false;

    Runnable mStartSong = new Runnable() {
        @Override
        public void run() {
            mStartSongRunning = true;

            Log.d(LOG_TAG , "mStartSong called");
                if(mIsPlaying){
                    //TODO: Switch the song
                } else {
                    //TODO: uncomment these after it's safe
                    startPlaying();
                }

            mStartSongRunning = false;
        }
    };

    private boolean mThreadRunning;
    private void writeToTrack(){
        mThreadRunning = true;
        Log.d(LOG_TAG , "Starting Write");
        while(isPlaying()) {
            if (mFrameToPlay == mLastFrameID) {
                mIsPlaying = false;
                return;
            }



            AudioFrame frame;
            synchronized (mFrames) {
                frame = mFrames.get(mFrameToPlay);
            }

            if (frame == null) {
                Log.d(LOG_TAG, "Frame ID is: " + mFrameToPlay + " , waiting");
                synchronized (this) {
                    while (!mFrames.containsKey(mFrameToPlay)) {
                        try {
                            this.wait(1);
                        } catch (InterruptedException e) {

                        }
                        if(!isPlaying()){
                            return;
                        }
                    }
                }
                frame = mFrames.get(mFrameToPlay);
                Log.d(LOG_TAG , "Frame #" + mFrameToPlay + " found.");
            }

            mLastFrameTime = frame.getPlayTime();
            mFrameToPlay++;


            long difference = mTimeManager.getPCMDifference(frame);


//            Log.d(LOG_TAG , frame.toString());
//
//            Log.d(LOG_TAG , "Difference is :" + difference);

            if(difference <= -30){
                long before = System.currentTimeMillis();
                synchronized (this) {
                    try {
                        this.wait(Math.abs(difference));
                    } catch (InterruptedException e) {

                    }
                }
            } else {
                int index = mFrameToPlay;

                while (difference >= 30) {
                    AudioFrame nextFrame = null;
                    synchronized (mFrames) {
                        nextFrame = mFrames.get(index);
                    }
                    if (nextFrame != null) {
                        difference = mTimeManager.getPCMDifference(nextFrame);
                        index++;
//                        Log.d(LOG_TAG , "Difference v2 is : " + difference);
                    } else if(nextFrame == null){
                        synchronized (this){
                            try {
                                this.wait(4);
                            } catch (InterruptedException e){

                            }
                            if(!isPlaying()){
                                return;
                            }
                        }
                    }
                }
                mFrameToPlay = index;
            }




            //TODO: handle it when this is null AND when the stream is over
            byte[] data = frame.getData();

            mTest++;

//            if (mTest >= 100) {
//                mTest = 0;
//                difference = mTimeManager.getPCMDifference(frame);
//                Log.d(LOG_TAG, "Time difference is : " + difference);
//            }


            mAudioTrack.write(data, 0, data.length);

            synchronized (mFrames){
                mFrames.remove(frame);
            }
        }
        Log.d(LOG_TAG , "Write Thread done");
        mThreadRunning = false;
    }
    //Takes in some frames, then waits for mFrames to be open and writes it to it
    public void addFrames(ArrayList<AudioFrame> frames){
        synchronized (mFrames){
            for(AudioFrame frame : frames){
                int ID = frame.getID();
                mFrames.put( ID , frame);
                mLastAddedFrameID = ID;
            }
        }
    }

    public AudioFrame getFrame(int ID){
        synchronized (mFrames) {
            return mFrames.get(ID);
        }
    }

    private Thread getWriteThread(){
        return new Thread(new Runnable()  {
            public void run() {
                writeToTrack();
            }
        });
    }

    private int mTest = 0;

    public void stopPlaying(){
        mIsPlaying = false;
    }

    public void startSong(long songStart){


        mFrames = new HashMap<>();
        mSongStartTime = songStart;
        double millisTillSongStart =  (songStart - mTimeManager.getOffset()) - System.currentTimeMillis();
        Log.d(LOG_TAG , "Milliseconds until song start: " + millisTillSongStart + " and mTimeManager.getOffset() is :" + mTimeManager.getOffset());
        mHandler.post(mStartSong);
    }

    private void startPlaying(){
        Log.d(LOG_TAG, "Write Started, difference is: " + (System.currentTimeMillis() - (mSongStartTime - mTimeManager.getOffset()))+ " mTimeManager.getOffset() is : " + mTimeManager.getOffset());

        createAudioTrack(44100 , 2);
        mAudioTrack.play();
        mIsPlaying = true;
        mWriteThread = getWriteThread();
        mWriteThread.start();
    }

    public boolean isPlaying(){
        return mIsPlaying;
    }

    private void createAudioTrack(int sampleRate , int channels){
        Log.d(LOG_TAG , "Creating Audio Track sampleRate : " + sampleRate + " and channels " + channels);
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channels, AudioFormat.ENCODING_PCM_16BIT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
    }

    public void setAudioTrack(AudioTrack track){
        mAudioTrack = track;
    }

    public void release(){
        if(mAudioTrack != null){
            mAudioTrack.flush();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }
    public void seek(long seekTime){
        mSeek = true;
        mFrameToPlay = (int)(seekTime / (1024000.0 / 44100.0));
        Log.d(LOG_TAG , "mFrameToPlay is : " + mFrameToPlay);
    }

    public void setLastFrameID(int lastFrame){
        mLastFrameID = lastFrame;
    }

    public void lastPacket(){
        mLastFrameID = mLastAddedFrameID;
    }

    public long pause(){
        Log.d(LOG_TAG , "Pausing AudioTrack");
        mIsPlaying = false;
        while(mThreadRunning){}
        return mLastFrameTime;
    }

    private boolean containsFrame(long seekTime){
        boolean contains = false;

        for(int i = 0; i < mFrames.size(); i++){
            long diff = mFrames.get(i).getPlayTime() - seekTime;

            if(Math.abs(diff) <= 22){
                contains = true;
                mFrameToPlay = i;
                break;
            }
        }
        return contains;
    }


    public void resume(long resumeTime){
        if(!mSeek){
            synchronized (mFrames){
                Log.d(LOG_TAG, mFrames.size() + " ");
                for (int i = 0; i < mFrames.size(); i++) {

                    long diff = mFrames.get(i).getPlayTime() - resumeTime;

                    if (Math.abs(diff) <= 22) {
                        mFrameToPlay = i;
                        break;
                    }
                }
            }
        } else {
            Log.d(LOG_TAG , "Difference between target frame play time and now is : " + (System.currentTimeMillis() - mTimeManager.getAACPlayTime(mFrameToPlay)));
            //TODO: Delete this comment
//            mSeek = false;
        }

        Log.d(LOG_TAG , "Resuming, mFrameToPlay is : " + mFrameToPlay);

        mIsPlaying = true;
        mWriteThread = getWriteThread();
        mWriteThread.start();
    }

    public void destroy(){
        Log.d(LOG_TAG , "AudioTrackManager Destroy Called");
        mIsPlaying = false;
        mWriteThread = null;
        if(mStartSongRunning){
            while(mStartSongRunning){

            }
        }
        mTimeManager = null;
        mHandler.removeCallbacks(mStartSong);
        mAudioTrack = null;
        mFrames = null;

    }

    public void newSong(){
        mIsPlaying = false;
    }
}