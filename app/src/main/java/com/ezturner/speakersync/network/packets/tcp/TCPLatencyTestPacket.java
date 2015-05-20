package com.ezturner.speakersync.network.packets.tcp;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.TimeManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by ezturner on 5/13/2015.
 */
public class TCPLatencyTestPacket {

    private Long mLatency;

    public TCPLatencyTestPacket(InputStream stream, TimeManager manager){
        receive(stream , manager);
    }

    public static void send(OutputStream stream , TimeManager manager){

        byte[] data = ByteBuffer.allocate(8).putLong(System.currentTimeMillis() + manager.getOffset()).array();



        synchronized (stream) {
            try {
                stream.write(CONSTANTS.TCP_LATENCY_TEST);
                stream.write(data);
                stream.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void receive(InputStream stream , TimeManager manager){
        byte[] data = new byte[8];

        synchronized (stream) {
            try {
                NetworkUtilities.readFromStream(stream , data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        mLatency = System.currentTimeMillis() - ByteBuffer.wrap(data).getLong() - manager.getOffset();

    }

    public Long getLatency(){
        return mLatency;
    }
}
