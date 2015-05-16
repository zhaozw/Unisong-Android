package com.ezturner.speakersync.network.packets.tcp;

import android.util.Log;

import com.ezturner.speakersync.network.CONSTANTS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Ethan on 5/7/2015.
 */
public class TCPRetransmitPacket {

    private static final String LOG_TAG = TCPRetransmitPacket.class.getSimpleName();
    
    //The packet to retransmit
    private int mPacketID;

    public TCPRetransmitPacket (InputStream stream){
        receive(stream);
    }

    public static void send(OutputStream stream , int ID){
        byte[] arr = ByteBuffer.allocate(4).putInt(ID).array();


        synchronized (stream){
            try {
                stream.write(CONSTANTS.TCP_COMMAND_RETRANSMIT);
                stream.write(arr);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void receive(InputStream stream){
        byte[] arr = new byte[4];

        synchronized (stream){
            try {
                stream.read(arr);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        mPacketID = ByteBuffer.wrap(arr).getInt();

    }

    public int getPacketToRetransmit(){
        return mPacketID;
    }

}
