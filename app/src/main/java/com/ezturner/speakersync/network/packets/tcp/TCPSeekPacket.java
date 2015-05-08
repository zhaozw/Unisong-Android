package com.ezturner.speakersync.network.packets.tcp;

import com.ezturner.speakersync.network.CONSTANTS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * The packet for seeking from one time in a song to another
 * Created by ezturner on 5/5/2015.
 */
public class TCPSeekPacket {

    //The frame that the host will seek to
    private int mFrameToDecodeNext;

    public TCPSeekPacket(InputStream stream){
        receive(stream);
    }

    public static void send(OutputStream stream, int frameToDecodeNext){
        byte[] data = ByteBuffer.allocate(4).putInt(frameToDecodeNext).array();

        synchronized (stream) {
            try {
                stream.write(CONSTANTS.TCP_SEEK);
                stream.write(data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void receive(InputStream stream){
        byte[] data = new byte[4];

        synchronized (stream) {
            try {
                stream.read(data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        mFrameToDecodeNext = ByteBuffer.wrap(data).getInt();

    }

    public int getFrameToDecodeNext(){
        return mFrameToDecodeNext;
    }
}
