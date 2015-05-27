package com.ezturner.speakersync.audio;

import com.ezturner.speakersync.network.master.Broadcaster;

import java.util.ArrayList;

/**
 * Created by Ethan on 4/27/2015.
 */
public class BroadcasterBridge extends AbstractBridge {

    private Broadcaster mBroadcaster;

    public BroadcasterBridge(Broadcaster broadcaster){
        super();
        mBroadcaster = broadcaster;
    }

    @Override
    protected void sendOutFrames(ArrayList<AudioFrame> frames) {
        mBroadcaster.addFrames(frames);
    }

    public void setAudioInfo(int channels){
        mBroadcaster.setAudioInfo(channels);
    }

    public void destroy(){
        mBroadcaster = null;
        super.destroy();
    }

    public void lastFrame(){
        mBroadcaster.lastPacket();
    }
}
