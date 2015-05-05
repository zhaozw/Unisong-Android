package com.ezturner.speakersync.network.packets.tcp;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ezturner on 5/5/2015.
 */
public class TCPPausePacket {

    //The time that all of the hosts will pause at
    private long mPauseTime;
    public TCPPausePacket(InputStream stream){
        receive(stream);
    }

    public static void send(OutputStream stream, long pauseTime){

    }

    private void receive(InputStream stream){

    }
}
