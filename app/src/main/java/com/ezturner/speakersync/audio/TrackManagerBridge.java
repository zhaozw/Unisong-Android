package com.ezturner.speakersync.audio;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by ezturner on 3/11/2015.
 */
public class TrackManagerBridge {

    private Queue<AudioFrame> mInputFrames;
    private boolean mInputReady;

    private Queue<AudioFrame> mOutputFrames;
    private boolean mOutputReady;


    private boolean mIsRunning;

    private AudioTrackManager mManager;

    public TrackManagerBridge(AudioTrackManager manager){

        mManager = manager;

        mIsRunning = true;

        mInputFrames = new LinkedList<AudioFrame>();
        mInputReady = false;

        mOutputFrames = new LinkedList<AudioFrame>();
        mOutputReady = false;

    }

    public void addFrame(AudioFrame frame){
        synchronized(mInputFrames){
            mInputFrames.add(frame);
        }
        mInputReady = true;
        notifyAll();
    }

    private void takeAllInputFrames(){
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
        notifyAll();

    }

    private Thread getInputThread(){
        return new Thread(){
            public void run(){
                while(mIsRunning ){
                    if(mInputReady) {
                        takeAllInputFrames();
                    }
                    try {
                        wait();
                    } catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private Thread getOutputThread(){
        return new Thread(){
            public void run(){
                while(mIsRunning ){
                    if(mOutputReady){
                        sendAllOutputFrames();
                    }

                    try {
                        wait();
                    } catch(InterruptedException e){
                        e.printStackTrace();
                    }

                }
            }
        };
    }

    private void sendAllOutputFrames(){
        mOutputReady = false;
        ArrayList<AudioFrame> frames = new ArrayList<AudioFrame>();
        synchronized (mOutputFrames){
            while(!mOutputFrames.isEmpty()){
                frames.add(mOutputFrames.poll());
            }
        }

        mManager.addFrames(frames);

    }


    public void stopBridge(){
        mIsRunning = false;
    }

    public void createAudioTrack(int sampleRate){
        mManager.createAudioTrack(sampleRate);
    }

    public void startSong(long startTime){
        mManager.startSong(startTime);
    }

}
