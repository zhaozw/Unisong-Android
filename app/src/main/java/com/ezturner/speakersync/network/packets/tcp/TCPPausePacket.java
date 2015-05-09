package com.ezturner.speakersync.network.packets.tcp;

import com.ezturner.speakersync.network.CONSTANTS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by ezturner on 5/5/2015.
 */
public class TCPPausePacket {

    //The time that all of the hosts will pause at
    private int mFrameToPlayNext;

    public TCPPausePacket(InputStream stream){
        receive(stream);
    }

    public static void send(OutputStream stream) {

        synchronized (stream) {
            try {
                stream.write(CONSTANTS.TCP_PAUSE);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void receive(InputStream stream){


    }
}
