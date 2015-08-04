package com.ezturner.speakersync.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

import com.ezturner.speakersync.MediaService;
import com.ezturner.speakersync.MyApplication;
import com.ezturner.speakersync.audio.master.FileDecoder;
import com.ezturner.speakersync.network.TimeManager;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioTrackManager implements AudioObserver {

    private static AudioTrackManager sInstance;

    public static AudioTrackManager getInstance(){
        if(sInstance == null){
            sInstance = new AudioTrackManager();
        }
        return sInstance;
    }

    //TODO: change this to AudioTrackManager.class.getSimpleName()
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

    //The last frame added's ID
    private int mLastAddedFrameID;

    //The class that handles all of the time calculations
    private TimeManager mTimeManager;

    //The boolean telling us if we have received a seek command
    private boolean mSeek = false;

    private long mLastFrameTime;

    private AudioStatePublisher mAudioStatePublisher;

    private FileDecoder mDecoder;

    private long mTimeUntilSongStart;

    //TODO: Make AudioTrack configuration dynamic with the details of the file
    public AudioTrackManager(){
        mFrames = new TreeMap<>();

        mThreadRunning = false;
        mIsPlaying = false;

        mFrameToPlay = 0;

        mTimeManager = TimeManager.getInstance();

        mLastFrameID = -1;
        mLastFrameTime = 0;

        //Get the AudioStatePublisher and then add ourselves to it
        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mAudioStatePublisher.attach(this);

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

    private int mRunCount;
    private void writeToTrack(){
        mThreadRunning = true;
        mRunCount = 0;

        //Wait until it's time, and then start the song

        try {
            synchronized (this) {
                this.wait(Math.abs(mTimeUntilSongStart));
            }
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        mAudioTrack.play();
        Log.d(LOG_TAG , "Starting Write");
        while(isPlaying()) {

            if (mFrameToPlay == mLastFrameID) {
                mIsPlaying = false;
                return;
            }

            /*
            if(!mFrames.containsKey(mFrameToPlay)){

                String set = "";
                for (Integer name: mFrames.keySet()) {

                    set += name.toString() + " , ";

                }
                Log.d(LOG_TAG , set.substring(0 , set.length() -1));
                Log.d(LOG_TAG , "mFrames size is :" + mFrames.size());

            }*/

            while (!mFrames.containsKey(mFrameToPlay)) {
                try {
                    synchronized (this) {
                        this.wait(1);
                    }
                } catch (InterruptedException e) {

                }
                if (!isPlaying()) {
                    return;
                }
            }

            AudioFrame frame;
            synchronized (mFrames) {
                frame = mFrames.get(mFrameToPlay);
                mFrames.remove(mFrameToPlay);
            }
            long difference = mTimeManager.getPCMDifference(frame.getPlayTime());

            /*
            //TODO :take a look at this code and see if we can improve it to limit audio skips/the artifacts I hear sometimes
            // note : this was causing the artifacts, and it looks like they stay in sync pretty well. Revisit this if they at times get out of sync.
            mRunCount++;
            if (mRunCount > 10) {
                mRunCount = 0;
//                Log.d(LOG_TAG , "Checking Difference: " + difference);
                if (difference <= -30) {

                    synchronized (this) {
                        try {
                            this.wait(Math.abs(difference));
//                            Log.d(LOG_TAG , "Waiting " + difference + "ms");
                        } catch (InterruptedException e) {

                        }
                    }
                } else {
                    int index = mFrameToPlay;

                    while (difference >= 30) {
                        index++;

                        AudioFrame nextFrame = null;
                        synchronized (mFrames) {
                            nextFrame = mFrames.get(index);
                            mFrames.remove(index);
                        }

                        if (nextFrame != null) {
                            difference = mTimeManager.getPCMDifference(nextFrame.getPlayTime());
                        } else {
                            synchronized (this) {
                                try {
                                    this.wait(4);
                                } catch (InterruptedException e) {

                                }
                            }

                            if (!isPlaying()) return;

                        }
                    }
//                    Log.d(LOG_TAG , "Skipped " + (index - mFrameToPlay) + " frames.");
                    mFrameToPlay = index;
                }
            }*/

            mLastFrameTime = frame.getPlayTime();
            mFrameToPlay++;

            byte[] data = frame.getData();


            mAudioTrack.write(data, 0, data.length);
        }


        mAudioTrack = null;
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

    public void startSong(){
        mFrames = new TreeMap<>();
        //TODO: fix this so that we have a decent way of configuring this differently for client/master
        mDecoder = new FileDecoder(MediaService.TEST_FILE_PATH , mFrames , 0 , this);
        double millisTillSongStart =  (mTimeManager.getSongStartTime() - mTimeManager.getOffset()) - System.currentTimeMillis();
        mTimeUntilSongStart = (long) millisTillSongStart;
        Log.d(LOG_TAG , "Milliseconds until song start: " + millisTillSongStart + " and mTimeManager.getOffset() is :" + mTimeManager.getOffset());
        mHandler.post(mStartSong);
    }

    private void startPlaying() {
        Log.d(LOG_TAG, "Write Started, difference is: " + (System.currentTimeMillis() - (mTimeManager.getSongStartTime() - mTimeManager.getOffset())) + " mTimeManager.getOffset() is : " + mTimeManager.getOffset());

        createAudioTrack(44100 , 2);
        mIsPlaying = true;
        mWriteThread = getWriteThread();
        mWriteThread.start();
    }

    public boolean isPlaying(){
        return mIsPlaying;
    }

    public void createAudioTrack(int sampleRate , int channels){
        if(mAudioTrack == null) {
            Log.d(LOG_TAG, "Creating Audio Track sampleRate : " + sampleRate + " and channels " + channels);
            int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channels, AudioFormat.ENCODING_PCM_16BIT);

            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        }
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
        mDecoder.destroy();
        mDecoder = new FileDecoder(MediaService.TEST_FILE_PATH , mFrames , seekTime , this);
        Log.d(LOG_TAG , "mFrameToPlay is : " + mFrameToPlay);
    }

    public void setLastFrameID(int lastFrame){
        mLastFrameID = lastFrame;
    }

    public void lastPacket(){
        mLastFrameID = mLastAddedFrameID;
    }

    public void pause(){
        if(mIsPlaying) {
            Log.d(LOG_TAG, "Pausing AudioTrack");
            mIsPlaying = false;
            while (mThreadRunning) {
            }
        }
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

    // The function that receives updates from the AudioStatePublisher
    // This function is how we know what the user is doing in regards to pause/skip/resume
    @Override
    public void update(int state){
        switch (state){

            case AudioStatePublisher.IDLE:
                mIsPlaying = false;
                break;

            case AudioStatePublisher.RESUME:
                long resumeTime = mAudioStatePublisher.getResumeTime();
                resume(resumeTime);
                break;

            case AudioStatePublisher.PAUSED:
                pause();
                break;

            case AudioStatePublisher.SEEK:
                pause();
                long seekTime = mAudioStatePublisher.getSeekTime();
                seek(seekTime);

                resume(mAudioStatePublisher.getResumeTime());
                break;
        }
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

    public long getLastFrameTime(){
        return  mLastFrameTime;
    }
}