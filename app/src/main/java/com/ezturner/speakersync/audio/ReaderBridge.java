package com.ezturner.speakersync.audio;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Ethan on 3/12/2015.
 */
public abstract class ReaderBridge {
    protected Queue<AudioFrame> mInputFrames;
    protected Thread mInputThread;
    protected boolean mInputReady;

    protected Queue<AudioFrame> mOutputFrames;
    protected Thread mOutputThread;
    protected boolean mOutputReady;


    private boolean mIsRunning;

    public ReaderBridge(){

        mIsRunning = true;

        mInputFrames = new LinkedList<AudioFrame>();
        mInputThread = getInputThread();
        mInputThread.start();
        mInputReady = false;

        mOutputFrames = new LinkedList<AudioFrame>();
        mOutputThread = getOutputThread();
        mOutputThread.start();
        mOutputReady = false;
    }

    public void addFrame(AudioFrame frame){
        synchronized(mInputFrames){
            mInputFrames.add(frame);
        }
        mInputReady = true;
        synchronized(mInputThread) {
            mInputThread.notify();
        }
    }

    protected void takeAllInputFrames(){
        mInputReady = false;
        ArrayList<AudioFrame> frames = new ArrayList<AudioFrame>();
        synchronized (mInputFrames){
            while(!mInputFrames.isEmpty()){
                frames.add(mInputFrames.poll());
            }
        }

        synchronized(mOutputFrames){
            for(AudioFrame frame : frames){
                mOutputFrames.add(frame);
            }
        }
        mOutputReady = true;
        synchronized(mOutputThread) {
            mOutputThread.notify();
        }
    }

    protected Thread getInputThread(){
        return new Thread(){
            public void run(){
                while(mIsRunning ){
                    if(mInputReady) {
                        takeAllInputFrames();
                    }
                    try {
                        Thread.sleep(200);
                    } catch(InterruptedException e){

                    }
                }
            }
        };
    }

    protected Thread getOutputThread(){
        return new Thread(){
            public void run(){
                while(mIsRunning ){
                    if(mOutputReady){
                        sendAllOutputFrames();
                    }

                    try {
                        Thread.sleep(200);
                    } catch(InterruptedException e){

                    }

                }
            }
        };
    }

    protected void sendAllOutputFrames(){
        mOutputReady = false;
        ArrayList<AudioFrame> frames = new ArrayList<AudioFrame>();
        synchronized (mOutputFrames){
            while(!mOutputFrames.isEmpty()){
                frames.add(mOutputFrames.poll());
            }
        }
        sendOutFrames(frames);

    }

    //This will be overriden in the subclass
    protected abstract  void sendOutFrames(ArrayList<AudioFrame> frames);


    public void stopBridge(){
        mIsRunning = false;
    }
}
