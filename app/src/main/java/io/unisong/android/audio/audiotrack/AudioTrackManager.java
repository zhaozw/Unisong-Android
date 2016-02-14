package io.unisong.android.audio.audiotrack;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.song.Song;
import io.unisong.android.network.ntp.TimeManager;
import io.unisong.android.network.session.UnisongSession;

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
    
    private String LOG_TAG = AudioTrackManager.class.getSimpleName();


    //the last frame ID that has been played
    private int frameToPlay;

    // whether we are playing, and whether the thread is waiting
    private boolean isPlaying = false, seek = false;

    //The thread that the writing will go on
    private AudioTrackThread audioThread;

    //The handler for scheduling resyncs and the song start
    private Handler handler;

    //The class that handles all of the time calculations
    private TimeManager timeManager;


    private long lastFrameTime;

    private AudioStatePublisher audioStatePublisher;

    // The song we're currently playing.
    private Song song;

    //TODO: Make AudioTrack configuration dynamic with the details of the file
    public AudioTrackManager(){

        frameToPlay = 0;

        timeManager = TimeManager.getInstance();

        lastFrameTime = 0;

        //Get the AudioStatePublisher and then add ourselves to it
        audioStatePublisher = AudioStatePublisher.getInstance();
        audioStatePublisher.attach(this);


        try {
            handler = new Handler();
        } catch (RuntimeException e){
            Looper.prepare();
            handler = new Handler();
        }
        instance = this;

        Log.d(LOG_TAG, "AudioTrackManager created and attached to AudioStatePublisher");
    }

    public void startSong(Song song){
        Log.d(LOG_TAG, "Write Started, difference is: " + ((timeManager.getSongStartTime()) - System.currentTimeMillis()) + " timeManager.getOffset() is : " + timeManager.getOffset());

        this.song = song;

        if(audioThread != null && audioThread.isRunning())
            audioThread.stopPlaying();

        Log.d(LOG_TAG , "song is : " + song.getName());
        // TODO : get info from song
        audioThread = new AudioTrackThread(createAudioTrack(44100, 2) , song, 0);
        audioThread.start();
    }

    public AudioTrack createAudioTrack(int sampleRate , int channels){
        Log.d(LOG_TAG, "Creating Audio Track sampleRate : " + sampleRate + " and channels " + channels);
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channels, AudioFormat.ENCODING_PCM_16BIT);

        return new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

    }

    public void pause(){
        Log.d(LOG_TAG, "Pausing AudioTrack");
        if(audioThread != null)
            audioThread.stopPlaying();
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

            case AudioStatePublisher.PLAYING:
                // TODO : replace with START_SONG
                Log.d(LOG_TAG , "Playing Received");
                if(!isPlaying){
                    if(timeManager == null)
                        timeManager = TimeManager.getInstance();
                    resume(timeManager.getSongTime() + 500);
                }
                break;
            case AudioStatePublisher.START_SONG:
                Log.d(LOG_TAG , "Starting song for AudioTrack");
                UnisongSession session = UnisongSession.getCurrentSession();
                try {
                    startSong(session.getCurrentSong());
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case AudioStatePublisher.END_SONG:
                audioThread.stopPlaying();
                isPlaying = false;
                break;
        }
    }

    public void resume(long resumeTime){

        if(seek && frameToPlay == -1) {
            frameToPlay = 0;
            seek = false;
        }

        if(audioThread!= null && audioThread.isRunning())
            audioThread.stopPlaying();

        Log.d(LOG_TAG , "Resuming, frameToPlay is : " + frameToPlay);

        if(song == null)
            song = UnisongSession.getCurrentSession().getCurrentSong();

        isPlaying = true;
        audioThread = new AudioTrackThread(createAudioTrack(44100, 2), song, resumeTime);
        audioThread.start();
    }

    public void destroy(){
        Log.d(LOG_TAG , "AudioTrackManager Destroy Called");
        isPlaying = false;
        if(audioThread != null)
            audioThread.stopPlaying();

        audioThread = null;
        timeManager = null;
        song = null;

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