package com.ezturner.speakersync.network.packets.tcp;

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

    public TCPPausePacket(InputStream stream) throws IOException{
        receive(stream);
    }

    public static void send(OutputStream stream, int frameToPlayNext) throws IOException{
        byte[] data = ByteBuffer.allocate(4).putInt(frameToPlayNext).array();

        stream.write(data);

    }

    private void receive(InputStream stream) throws IOException{
        byte[] data = new byte[4];

        stream.read(data);

        mFrameToPlayNext = ByteBuffer.wrap(data).getInt();

    }

    public int getFrameToPlayNext(){
        return mFrameToPlayNext;
    }
}
