package com.ezturner.speakersync.network.master.transmitter;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.audio.master.AACEncoder;

import java.util.Map;

/**
 * This class will handle point to point transmissions in an environment where multicast/broadcast does
 * not fully propagate across the LAN
 * Created by Ethan on 5/22/2015.
 */
public class TCPTransmitter implements Transmitter {

    public TCPTransmitter(){

    }

    public void addSlave(){

    }


    @Override
    public void setAACEncoder(AACEncoder encoder) {

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
