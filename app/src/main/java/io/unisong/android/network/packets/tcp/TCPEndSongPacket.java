package io.unisong.android.network.packets.tcp;

import io.unisong.android.network.CONSTANTS;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Ethan on 5/29/2015.
 */
public class TCPEndSongPacket {

    public static void send(OutputStream stream, byte streamID) {

        synchronized (stream) {
            try {
                stream.write(CONSTANTS.TCP_END_SONG);
                stream.write(streamID);
                stream.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

    }

}
