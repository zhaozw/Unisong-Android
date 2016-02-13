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
import io.unisong.android.audio.song.Song;
import io.unisong.android.audio.song.SongFormat;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.ntp.TimeManager;
import io.unisong.android.network.session.UnisongSession;

/**
 * The class to handle transmissions to the backend
 * Created by Ethan on 5/22/2015.
 */
public class ServerTransmitter implements Transmitter, AudioObserver {

    private final static String LOG_TAG = ServerTransmitter.class.getSimpleName();

    private SocketIOClient client;
    private boolean transmitting;
    private AudioStatePublisher publisher;
    private TimeManager timeManager;
    private Integer frameToUpload;
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService worker;
    private UnisongSession session;

    public ServerTransmitter(UnisongSession session){
        timeManager = TimeManager.getInstance();
        publisher = AudioStatePublisher.getInstance();
        publisher.attach(this);
        client = SocketIOClient.getInstance();
        worker = Executors.newSingleThreadScheduledExecutor();
        this.session = session;
    }


    private boolean stop = false;
    private Song song;

    private void broadcast(){
        int currentFrame = 0;

        Song currentSong = song;
        transmitting = true;

        while(transmitting){
            if(!currentSong.hasAACFrame(currentFrame) && !stop){

                synchronized (this){
                    try{
                        this.wait(10);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

            } else if(stop){
                break;
            }

            AudioFrame frame = currentSong.getAACFrame(currentFrame);

            // TODO : figure out why frame is null
            if(frame == null){
                Log.d(LOG_TAG , "FRAME IS NULL WTF :"  + currentSong.hasAACFrame(currentFrame) );
                continue;
            }

            currentFrame++;

            uploadFrame(frame);
        }


        transmitting = false;
    }

    private void uploadFrame(AudioFrame frame){
        JSONObject obj = new JSONObject();

        try {
            obj.put("dataID", frame.getID());
            obj.put("data", frame.getData());
            obj.put("songID", frame.getSongID());
        } catch (JSONException e){
            e.printStackTrace();
            return;
        }

        client.emit("upload data", obj);
    }

    @Override
    public void startSong(Song song) {

        JSONObject startSongJSON = new JSONObject();

        try {
            startSongJSON.put("songStartTime", timeManager.getSongStartTime() + timeManager.getOffset());
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

        client.emit("start song", startSongJSON);

        frameToUpload = 0;
        this.song = song;
        Log.d(LOG_TAG , "Start song");

        if(scheduledFuture != null)
            scheduledFuture.cancel(false);

        scheduledFuture = worker.scheduleAtFixedRate(broadcastRunnable, 0, 5, TimeUnit.MILLISECONDS);
    }

    private int uploadCount = 0;

    private Runnable broadcastRunnable = () -> {

        if(song.hasAACFrame(frameToUpload)){
            AudioFrame frame = song.getAACFrame(frameToUpload);
//            Log.d(LOG_TAG, "Frame is null? " + (frame == null));
            if(frame != null) {
//                Log.d(LOG_TAG, "Frame #" + frameToUpload + " uploaded");
                uploadFrame(frame);

                if(uploadCount >= 100){
                    Log.d(LOG_TAG , "Frame #" + frameToUpload + " uploaded");
                }
                synchronized (frameToUpload) {
                    frameToUpload++;
                }
            }
        } else {
//            Log.d(LOG_TAG , "Looking for frame #" + frameToUpload + " while size is : " + song.getPCMFrames().size());
        }


    };

    @Override
    public void update(int state) {
        switch (state){
            case AudioStatePublisher.PAUSED:
                client.emit("pause", "pause");
                break;
            case AudioStatePublisher.RESUME:
                try {
                    JSONObject object = new JSONObject();
                    object.put("songID" , session.getCurrentSongID());
                    object.put("resumeTime", publisher.getResumeTime());
                    object.put("songStartTime", TimeManager.getInstance().getSongStartTime() + TimeManager.getInstance().getOffset());
                    client.emit("resume", object);
                } catch (JSONException e){
                    e.printStackTrace();
                }
                break;
            case AudioStatePublisher.SEEK:
                client.emit("seek", publisher.getSeekTime());
                frameToUpload = (int) (publisher.getSeekTime() / ((1024.0 * 1000.0) / 44100.0));
                break;
            case AudioStatePublisher.PLAYING:
//                client.emit("playing", "playing");
                // TODO : do we need this^^ ?
                break;
            case AudioStatePublisher.END_SONG:
                Log.d(LOG_TAG , "Emitting end song");
                client.emit("end song", publisher.getSongToEnd());
                break;
            case AudioStatePublisher.START_SONG:
                startSong(session.getCurrentSong());
                break;
        }
    }

    public void destroy(){

    }
}
