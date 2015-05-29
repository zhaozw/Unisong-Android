package com.ezturner.speakersync.audio;

import android.util.Log;

import com.ezturner.speakersync.network.master.Broadcaster;

import java.util.ArrayList;

/**
 * Created by Ethan on 4/27/2015.
 */
public class BroadcasterBridge extends AbstractBridge {

    private final String LOG_TAG = BroadcasterBridge.class.getSimpleName();

    private Broadcaster mBroadcaster;

    public BroadcasterBridge(Broadcaster broadcaster){
        super();
        mBroadcaster = broadcaster;
    }

    @Override
    protected void sendOutFrames(ArrayList<AudioFrame> frames) {
//        Log.d(LOG_TAG , "Sending out " + frames.size() + " frames.");
        mBroadcaster.addFrames(frames);
    }


    @Override
    protected Thread getThread(){
        return new Thread(){
            public void run(){
                while(mIsRunning ){
//                    Log.d(LOG_TAG , "Thread running, mFrames size is :" + mFrames.size());
                    if(mFrames.size() != 0){
                        sendAllOutputFrames();
                    }

                    synchronized (this){
                        try {
                            this.sleep(5);
                        } catch(InterruptedException e){

                        }
                    }

                }
//                Log.d(LOG_TAG , "BroadcasterBridge thread over");
            }
        };
    }

    @Override
    public void addFrame(AudioFrame frame){
//        Log.d(LOG_TAG , "mFrames size add is :" + mFrames.size());
        synchronized(mFrames){
            mFrames.add(frame);
        }
    }

    public void setAudioInfo(int channels){
        mBroadcaster.setAudioInfo(channels);
    }

    public void destroy(){
        Log.d(LOG_TAG , "BroadcasterBridge destroyed.");
        mBroadcaster = null;
        super.destroy();
    }

    public Broadcaster getBroadcaster(){
        return mBroadcaster;
    }

    public void lastFrame(){
        mBroadcaster.lastPacket();
    }
}
