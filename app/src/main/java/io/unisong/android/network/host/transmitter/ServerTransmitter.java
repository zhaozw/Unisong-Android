package io.unisong.android.network.host.transmitter;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.ntp.TimeManager;
import io.unisong.android.network.song.Song;
import io.unisong.android.network.song.SongFormat;

/**
 * The class to handle transmissions to the backend
 * Created by Ethan on 5/22/2015.
 */
public class ServerTransmitter implements Transmitter, AudioObserver {

    private final static String LOG_TAG = ServerTransmitter.class.getSimpleName();

    private SocketIOClient mClient;
    private boolean mTransmitting;
    private AudioStatePublisher mAudioStatePublisher;
    private TimeManager mTimeManager;
    private int mFrameToUpload;
    private ScheduledFuture<?> mScheduledFuture;
    private ScheduledExecutorService mWorker;

    public ServerTransmitter(){
        mTimeManager = TimeManager.getInstance();
        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mAudioStatePublisher.attach(this);
        mClient = SocketIOClient.getInstance();
        mWorker = Executors.newSingleThreadScheduledExecutor();
    }


    private boolean mStop = false;
    private Song mSong;

    private void broadcast(){
        int currentFrame = 0;

        Song currentSong = mSong;
        mTransmitting = true;

        while(mTransmitting){
            if(!currentSong.hasFrame(currentFrame) && !mStop){

                synchronized (this){
                    try{
                        this.wait(10);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

            } else if(mStop){
                break;
            }

            AudioFrame frame = currentSong.getFrame(currentFrame);

            // TODO : figure out why frame is null
            if(frame == null){
                Log.d(LOG_TAG , "FRAME IS NULL WTF :"  + currentSong.hasFrame(currentFrame) );
                continue;
            }

            currentFrame++;

            uploadFrame(frame);
        }


        mTransmitting = false;
    }

    private void uploadFrame(AudioFrame frame){
        JSONObject obj = new JSONObject();

        try {
            obj.put("dataID", frame.getID());
            obj.put("data", frame.getData());
            obj.put("songID", frame.getSongID());
        } catch (JSONException e){
            e.printStackTrace();
        }

        mClient.emit("upload data", obj);
    }

    @Override
    public void startSong(Song song) {

        JSONObject startSongJSON = new JSONObject();

        try {
            startSongJSON.put("songStartTime", mTimeManager.getSongStartTime() + mTimeManager.getOffset());
            startSongJSON.put("songID", song.getID());
            SongFormat format = song.getFormat();

            if(format != null)
                startSongJSON.put("format", format.toJSON());
        } catch (JSONException e){
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
            Log.d(LOG_TAG, "NullPointerException in startSong()! Was the SongFormat null?");
        }

        mClient.emit("start song" , startSongJSON);

        mFrameToUpload = 0;
        mSong = song;
        Log.d(LOG_TAG , "Start song x232");

        if(mScheduledFuture != null)
            mScheduledFuture.cancel(false);

        mScheduledFuture = mWorker.scheduleAtFixedRate(mBroadcastRunnable, 0, 5, TimeUnit.MILLISECONDS);
    }

    private int mUploadCount = 0;

    private Runnable mBroadcastRunnable = () -> {

        if(mSong.hasFrame(mFrameToUpload)){
            AudioFrame frame = mSong.getFrame(mFrameToUpload);
//            Log.d(LOG_TAG, "Frame is null? " + (frame == null));
            if(frame != null) {
//                Log.d(LOG_TAG, "Frame #" + mFrameToUpload + " uploaded");
                uploadFrame(frame);

                if(mUploadCount >= 100){
                    Log.d(LOG_TAG , "Frame #" + mFrameToUpload + " uploaded");
                }
                mFrameToUpload++;
            }
        } else {
//            Log.d(LOG_TAG , "Looking for frame #" + mFrameToUpload + " while size is : " + mSong.getPCMFrames().size());
        }


    };

    @Override
    public void update(int state) {
        switch (state){
            case AudioStatePublisher.PAUSED:
                mClient.emit("pause", "pause");
                break;
            case AudioStatePublisher.RESUME:
                try {
                    JSONObject object = new JSONObject();
                    object.put("resumeTime", mAudioStatePublisher.getResumeTime());
                    object.put("songStartTime", TimeManager.getInstance().getSongStartTime() + TimeManager.getInstance().getOffset());
                    mClient.emit("resume", object);
                } catch (JSONException e){
                    e.printStackTrace();
                }
                break;
            case AudioStatePublisher.SEEK:
                mClient.emit("seek" , mAudioStatePublisher.getSeekTime());
                break;
            case AudioStatePublisher.PLAYING:
                mClient.emit("playing" , "playing");
                break;
            case AudioStatePublisher.END_SONG:
                mClient.emit("end song" , mAudioStatePublisher.getSongToEnd());
                break;
        }
    }

    public void destroy(){

    }
}
