package io.unisong.android.network.master.transmitter;

import android.util.Log;

import java.io.CharArrayReader;
import java.util.Map;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.master.AACEncoder;
import io.unisong.android.network.HttpClient;
import io.unisong.android.network.SocketIOClient;

/**
 * The class to handle transmissions to my python/HTTP server
 * Created by Ethan on 5/22/2015.
 */
public class ServerTransmitter implements Transmitter {

    private final static String LOG_TAG = ServerTransmitter.class.getSimpleName();

    private SocketIOClient mClient;
    private boolean mTransmitting;
    private AACEncoder mEncoder;
    private Map<Integer, AudioFrame> mFrames;
    private Thread mBroadcastThread;

    public ServerTransmitter(){
        mTransmitting = true;
        mBroadcastThread = getBroadcastThread();
        mBroadcastThread.start();
        HttpClient httpClient = HttpClient.getInstance();
        httpClient.login("anoaz" , "pass");
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
        while(mEncoder == null || mFrames == null){
            synchronized (this){
                try{
                    this.wait(50);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            if(mEncoder != null && mFrames == null){
                mFrames = mEncoder.getFrames();
            }
        }
        mClient = new SocketIOClient();
        mClient.joinSession(5);
        int i = 0;
        while(mTransmitting){
            AudioFrame frame;

            synchronized (mFrames) {
                frame  = mFrames.get(i);
            }

            if(frame == null){
                synchronized (this){
                    try{
                        this.wait(50);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
                continue;
            }
            i++;

            Log.d(LOG_TAG, "sending frame.");
            mClient.upload(frame);
            // TODO : implement a better solution other than raw mFrames access.
            mEncoder.frameUsed(i - 1);
            return;
        }
    }

    @Override
    public void setAACEncoder(AACEncoder encoder) {
        mFrames = encoder.getFrames();
        mEncoder = encoder;
    }

    @Override
    public void setLastFrame(int lastFrame) {

    }

    @Override
    public void startSong() {

    }

    @Override
    public void update(int state) {

    }
}
