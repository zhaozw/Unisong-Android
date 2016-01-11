package io.unisong.android.network.host.transmitter;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.TimeManager;
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
    private Thread mBroadcastThread;
    private AudioStatePublisher mAudioStatePublisher;
    private TimeManager mTimeManager;

    public ServerTransmitter(){
        mTimeManager = TimeManager.getInstance();
        mAudioStatePublisher = AudioStatePublisher.getInstance();
        mClient = SocketIOClient.getInstance();
    }

    private Thread getBroadcastThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {

                if(mTransmitting){
                    mStop = true;
                    while(mTransmitting) {
                        try {
                            synchronized (this) {
                                this.wait(5);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            broadcast();
        }
    });
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
                continue;
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

        //mClient.emit("upload data", obj);
    }

    @Override
    public void startSong(Song song) {

        JSONObject startSongJSON = new JSONObject();

        try {
            startSongJSON.put("songStartTime", mTimeManager.getSongStartTime());
            startSongJSON.put("songID", song.getID());
            SongFormat format = song.getFormat();

            // TODO : something. This will hang up the UI thread if it's any length at all.
            int runs = 50;
            while(format == null){

                runs--;
                if(runs <= 0)
                    break;

                synchronized (this){
                    try{
                        this.wait(1);
                    } catch (InterruptedException e){

                    }
                }
                format = song.getFormat();

            }

            if(format != null)
                startSongJSON.put("format", format.toJSON());
        } catch (JSONException e){
            e.printStackTrace();
        }

        mClient.emit("start song" , startSongJSON);

        mSong = song;
        mBroadcastThread = getBroadcastThread();
        mBroadcastThread.start();
    }

    @Override
    public void update(int state) {
        switch (state){
            case AudioStatePublisher.PAUSED:
                mClient.emit("pause", null);
                break;
            case AudioStatePublisher.RESUME:
                try {
                    JSONObject object = new JSONObject();
                    object.put("resumeTime", mAudioStatePublisher.getResumeTime());
                    object.put("songStartTime", TimeManager.getInstance().getSongStartTime());
                    mClient.emit("resume", object);
                } catch (JSONException e){
                    e.printStackTrace();
                }
                break;
            case AudioStatePublisher.SEEK:
                mClient.emit("seek" , mAudioStatePublisher.getSeekTime());
                break;
            case AudioStatePublisher.PLAYING:
                mClient.emit("playing" , null);
                break;
            case AudioStatePublisher.END_SONG:
                mClient.emit("end song" , mAudioStatePublisher.getSongToEnd());
                break;
        }
    }
}
