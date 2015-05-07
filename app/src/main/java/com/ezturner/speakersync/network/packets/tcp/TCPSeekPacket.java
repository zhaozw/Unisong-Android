package com.ezturner.speakersync.network.packets.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * The packet for seeking from one time in a song to another
 * Created by ezturner on 5/5/2015.
 */
public class TCPSeekPacket {

    //The time that all of the hosts will pause at
    private int mFrameToDecodeNext;

    public TCPSeekPacket(InputStream stream) throws IOException {
        receive(stream);
    }

    public static void send(OutputStream stream, int frameToDecodeNext) throws IOException{
        byte[] data = ByteBuffer.allocate(4).putInt(frameToDecodeNext).array();

        stream.write(data);

    }

    private void receive(InputStream stream) throws IOException{
        byte[] data = new byte[4];

        stream.read(data);

        mFrameToDecodeNext = ByteBuffer.wrap(data).getInt();

    }

    public int getFrameToDecodeNext(){
        return mFrameToDecodeNext;
    }
}
