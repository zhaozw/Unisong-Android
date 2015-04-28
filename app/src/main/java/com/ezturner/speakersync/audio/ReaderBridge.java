package com.ezturner.speakersync.audio;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Ethan on 3/12/2015.
 */
public abstract class ReaderBridge {

    protected Queue<AudioFrame> mFrames;
    protected Thread mThread;



    private boolean mIsRunning;

    public ReaderBridge(){
        mIsRunning = true;
        mFrames = new LinkedList<AudioFrame>();
        mThread = getThread();
        mThread.start();

    }

    public void addFrame(AudioFrame frame){
        synchronized(mFrames){
            mFrames.add(frame);
        }
    }

    protected Thread getThread(){
        return new Thread(){
            public void run(){
                while(mIsRunning ){
                    if(mFrames.size() != 0){
                        sendAllOutputFrames();
                    }

                    try {
                        Thread.sleep(5);
                    } catch(InterruptedException e){

                    }

                }
            }
        };
    }

    protected void sendAllOutputFrames(){
        ArrayList<AudioFrame> frames = new ArrayList<AudioFrame>();
        synchronized (mFrames){
            while(!mFrames.isEmpty()){
                frames.add(mFrames.poll());
            }
        }
        sendOutFrames(frames);
    }

    //This will be overriden in the subclass
    protected abstract  void sendOutFrames(ArrayList<AudioFrame> frames);

}
