package io.unisong.android.audio.audiotrack;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

import java.util.Date;
import java.util.Map;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.network.ntp.TimeManager;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.audio.song.Song;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioTrackManager implements AudioObserver {

    private static AudioTrackManager instance;

    public static AudioTrackManager getInstance(){
        if(instance == null){
            instance = new AudioTrackManager();
        }
        return instance;
    }

    //TODO: change this to AudioTrackManager.class.getSimpleName()
    private String LOG_TAG = "AudioTrackManager";

    //The AudioTrack used for playback
    private AudioTrack audioTrack;

    //the last frame ID that has been played
    private int frameToPlay;

    //boolean telling the Write thread to continue or not
    private boolean isPlaying;

    //The thread that the writing will go on
    private Thread writeThread;

    //The current last frame
    private int lastFrameID;

    //The handler for scheduling resyncs and the song start
    private Handler handler;

    //The class that handles all of the time calculations
    private TimeManager timeManager;

    // The time we will be resuming at.
    private long resumeTime;

    //The boolean telling us if we have received a seek command
    private boolean seek = false;

    private long lastFrameTime;

    private AudioStatePublisher audioStatePublisher;

    private long mTimeUntilSongStart;

    // The song we're currently playing.
    private Song song;

    //TODO: Make AudioTrack configuration dynamic with the details of the file
    public AudioTrackManager(){

        threadRunning = false;
        isPlaying = false;

        frameToPlay = 0;

        timeManager = TimeManager.getInstance();

        lastFrameID = -1;
        lastFrameTime = 0;

        //Get the AudioStatePublisher and then add ourselves to it
        audioStatePublisher = AudioStatePublisher.getInstance();
        audioStatePublisher.attach(this);


        handler = new Handler();
        instance = this;

        Log.d(LOG_TAG , "AudioTrackManager created and attached to AudioStatePublisher");
    }

    private boolean mStartSongRunning = false;

    Runnable mStartSong = new Runnable() {
        @Override
        public void run() {
            mStartSongRunning = true;

            Log.d(LOG_TAG , "mStartSong called");
                if(isPlaying){
                    //TODO: Switch the song
                } else {
                    //TODO: uncomment these after it's safe
                    startPlaying();
                }

            mStartSongRunning = false;
        }
    };

    private boolean threadRunning;

    private int runCount;
    private void writeToTrack(){
        threadRunning = true;
        runCount = 0;

        int count = 0;

        long waitTime = timeManager.getSongStartTime() + resumeTime - System.currentTimeMillis() ;
        Log.d(LOG_TAG, "Waiting for " + waitTime + "ms, and resume time is :" + resumeTime);
        try{

            if(waitTime > 0) {
                synchronized (this) {
                    this.wait(waitTime);
                }
            } else {
                Log.d(LOG_TAG , "Wait time was less than 0!");
            }
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        if(!song.started()){
            Log.d(LOG_TAG , "Song has not been started! Starting now.");
            song.start();
        }

        audioTrack.play();
        if(resumeTime == 0) {
            Log.d(LOG_TAG, "Starting Write, " + (timeManager.getSongStartTime() - System.currentTimeMillis()) + "ms until song start time.");
        } else {
            Log.d(LOG_TAG , "Starting Write, " + (timeManager.getSongStartTime() + resumeTime - System.currentTimeMillis())  + "ms until song start time." );
        }
        while(isPlaying()) {

            if (frameToPlay == lastFrameID) {
                isPlaying = false;
                return;
            }

            /*
            if(!mFrames.containsKey(frameToPlay)){

                String set = "";
                for (Integer name: mFrames.keySet()) {

                    set += name.toString() + " , ";

                }
                Log.d(LOG_TAG , set.substring(0 , set.length() -1));
                Log.d(LOG_TAG , "mFrames size is :" + mFrames.size());

            }*/

            if(!song.hasPCMFrame(frameToPlay)){
                Log.d(LOG_TAG , "Song does not have frame #" + frameToPlay);
            }

            boolean firstWait = true;
            while (!song.hasPCMFrame(frameToPlay)) {
                try {
                    synchronized (this) {
                        this.wait(1);
                    }
                } catch (InterruptedException e) {

                }
                if (!isPlaying()) {
                    Log.d(LOG_TAG , "We have stopped playing!");
                    return;
                }
            }

            AudioFrame frame = song.getPCMFrame(frameToPlay);

            count++;
            if(count >= 50){
                count = 0;
                long UTCplayTime = timeManager.getSongStartTime() + frame.getPlayTime();
                Log.d(LOG_TAG , "Time difference : " + (System.currentTimeMillis() - UTCplayTime) + " and playTime : " +  frame.getPlayTime());
                synchronizePlayTime(System.currentTimeMillis() - UTCplayTime);
            }

            lastFrameTime = frame.getPlayTime();
            frameToPlay++;

            byte[] data = frame.getData();


            audioTrack.write(data, 0, data.length);
        }


        Log.d(LOG_TAG , "Write Thread done, last frame #" + frameToPlay);
        threadRunning = false;
    }

    private Thread getWriteThread(){
        return new Thread(new Runnable()  {
            public void run() {
                // set this thread priority to high
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                writeToTrack();
            }
        });
    }

    public void startSong(Song song){

        resumeTime = 0;
        this.song = song;

        double millisTillSongStart =  timeManager.getSongStartTime() - System.currentTimeMillis();
        Log.d(LOG_TAG, "TimeStartTime : " + new Date(timeManager.getSongStartTime()).toString());
        Log.d(LOG_TAG, "Milliseconds until song start: " + millisTillSongStart + " and timeManager.getOffset() is :" + timeManager.getOffset());
        handler.post(mStartSong);
    }

    private void startPlaying() {
        Log.d(LOG_TAG, "Write Started, difference is: " + ((timeManager.getSongStartTime()) - System.currentTimeMillis()) + " timeManager.getOffset() is : " + timeManager.getOffset());

        // TODO : get info from song
        createAudioTrack(44100 , 2);
        isPlaying = true;
        writeThread = getWriteThread();
        writeThread.start();
    }

    public boolean isPlaying(){
        return isPlaying;
    }

    public void createAudioTrack(int sampleRate , int channels){
        if (audioTrack == null) {
            Log.d(LOG_TAG, "Creating Audio Track sampleRate : " + sampleRate + " and channels " + channels);
            int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channels, AudioFormat.ENCODING_PCM_16BIT);

            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        }
    }

    public void release(){
        if(audioTrack != null){
            audioTrack.flush();
            audioTrack.release();
            audioTrack = null;
        }
    }
    public void seek(long seekTime){
        seek = true;
//        frameToPlay = (int)(seekTime / (1024000.0 / 44100.0));
        Log.d(LOG_TAG , "frameToPlay is : " + frameToPlay);
    }

    public void setLastFrameID(int lastFrame){
        lastFrameID = lastFrame;
    }

    public void pause(){
        if(isPlaying) {
            Log.d(LOG_TAG, "Pausing AudioTrack");
            isPlaying = false;
            while (threadRunning) {
            }
        }
    }

    // The function that receives updates from the AudioStatePublisher
    // This function is how we know what the user is doing in regards to pause/skip/resume
    @Override
    public void update(int state){
        switch (state){

            case AudioStatePublisher.IDLE:
                isPlaying = false;
                break;

            case AudioStatePublisher.RESUME:
                Log.d(LOG_TAG , "AudioTrackManager resume received");
                long resumeTime = audioStatePublisher.getResumeTime();
                resume(resumeTime);
                break;

            case AudioStatePublisher.PAUSED:
                Log.d(LOG_TAG , "AudioTrackManager pause received");
                pause();
                break;

            case AudioStatePublisher.SEEK:
//                long seekTime = audioStatePublisher.getSeekTime();
//                seek(seekTime);
                seek = true;
                break;

            case AudioStatePublisher.PLAYING:
                Log.d(LOG_TAG , "Playing Received");
                UnisongSession session = UnisongSession.getCurrentSession();
                try {
                    startSong(session.getCurrentSong());
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;
        }
    }

    public void resume(long resumeTime){
        this.resumeTime = resumeTime;

        frameToPlay = -1;

        if(seek && frameToPlay == -1)
            frameToPlay = 0;


        Log.d(LOG_TAG , "Resuming, frameToPlay is : " + frameToPlay);

        isPlaying = true;
        writeThread = getWriteThread();
        writeThread.start();
    }

    public void destroy(){
        Log.d(LOG_TAG , "AudioTrackManager Destroy Called");
        isPlaying = false;
        writeThread = null;
        if(mStartSongRunning){
            while(mStartSongRunning){

            }
        }
        timeManager = null;
        handler.removeCallbacks(mStartSong);
        audioTrack = null;
        song = null;

    }

    public void newSong(){
        isPlaying = false;
    }

    public long getLastFrameTime(){
        return lastFrameTime;
    }

    private void synchronizePlayTime(long difference){

        /*
            //TODO :take a look at this code and see if we can improve it to limit audio skips/the artifacts I hear sometimes
            // note : this was causing the artifacts, and it looks like they stay in sync pretty well. Revisit this if they at times get out of sync.

        if (difference <= -30) {
            synchronized (this) {
                try {
                    this.wait(Math.abs(difference));
//                           Log.d(LOG_TAG , "Waiting " + difference + "ms");
                } catch (InterruptedException e) {
                }
            }
        } else {
            int index = frameToPlay;
            while (difference >= 30) {
                index++;
                // TODO : synchronize this?
                AudioFrame nextFrame = song.getPCMFrame(index);

                if (nextFrame != null) {
                    difference = timeManager.getPCMDifference(nextFrame.getPlayTime());
                } else {
                    synchronized (this) {
                        try {
                            this.wait(4);
                        } catch (InterruptedException e) {
                        }
                    }

                }
            }
//                   Log.d(LOG_TAG , "Skipped " + (index - frameToPlay) + " frames.");
            frameToPlay = index;
        }*/
    }
}