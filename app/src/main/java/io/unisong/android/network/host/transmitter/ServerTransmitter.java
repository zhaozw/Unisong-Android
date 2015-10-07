package io.unisong.android.network.host.transmitter;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.AudioSource;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.TimeManager;
import io.unisong.android.network.song.Song;

/**
 * The class to handle transmissions to my python/HTTP server
 * Created by Ethan on 5/22/2015.
 */
public class ServerTransmitter implements Transmitter, AudioObserver {

    private final static String LOG_TAG = ServerTransmitter.class.getSimpleName();

    private SocketIOClient mClient;
    private boolean mTransmitting;
    private AudioSource mSource;
    private Thread mBroadcastThread;
    private TimeManager mTimeManager;

    public ServerTransmitter(){
        mTimeManager = TimeManager.getInstance();
        mTransmitting = true;
        //HttpClient httpClient = HttpClient.getInstance();
        //httpClient.login("anoaz" , "pass");

        mClient = new SocketIOClient();
        mClient.joinSession(5);
    }

    private Thread getBroadcastThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                broadcast();
            }
        });
    }

    private void broadcast(){
        int currentFrame = 0;

        while(mTransmitting){
            if(!mSource.hasFrame(currentFrame)){

                synchronized (this){
                    try{
                        this.wait(10);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
                continue;
            }

            AudioFrame frame = mSource.getFrame(currentFrame);

            // TODO : figure out why frame is null
            if(frame == null){
                Log.d(LOG_TAG , "FRAME IS NULL WTF :"  + mSource.hasFrame(currentFrame) );
                continue;
            }
            mSource.frameUsed(currentFrame);

            currentFrame++;

            uploadFrame(frame);
        }
    }

    private void uploadFrame(AudioFrame frame){
        JSONObject obj = new JSONObject();

        try {
            obj.put("dataID", frame.getID());
            obj.put("data", frame.getData());
            obj.put("songID", 5);
        } catch (JSONException e){
            e.printStackTrace();
        }

        mClient.emit("upload data", obj);
    }

    @Override
    public void setAudioSource(AudioSource source) {
        mSource = source;
    }

    @Override
    public void setLastFrame(int lastFrame) {

    }

    @Override
    public void startSong(Song song) {

        JSONObject startSongJSON = new JSONObject();

        try {
            startSongJSON.put("songStartTime", mTimeManager.getSongStartTime());
            startSongJSON.put("songID", song.getID());
            startSongJSON.put("format" ,song.getFormat().toJSON());
        } catch (JSONException e){
            e.printStackTrace();
        }

        mClient.emit("start song" , startSongJSON);

        mBroadcastThread = getBroadcastThread();
        mBroadcastThread.start();
    }

    @Override
    public void update(int state) {
        switch (state){
            case AudioStatePublisher.PAUSED:
                break;
            case AudioStatePublisher.RESUME:
                break;
            case AudioStatePublisher.SEEK:
                break;
        }
    }
}
