package io.unisong.android.network.packets.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.unisong.android.network.CONSTANTS;

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
                stream.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void receive(InputStream stream){


    }
}
