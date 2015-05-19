package com.ezturner.speakersync.network.packets.tcp;

import android.util.Log;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Ethan on 5/7/2015.
 */
public class TCPRequestPacket {

    private static final String LOG_TAG = TCPRequestPacket.class.getSimpleName();

    //The packet that has been requested
    private int mPacketID;

    public TCPRequestPacket(InputStream stream){
        receive(stream);
    }

    public static void send(OutputStream stream , int packetID){

        byte[] data = ByteBuffer.allocate(4).putInt(packetID).array();



        synchronized (stream){
            try {
                stream.write(CONSTANTS.TCP_REQUEST);
                stream.write(data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void receive(InputStream stream){
        byte[] data = new byte[4];

        synchronized (stream){
            try {
                NetworkUtilities.readFromStream(stream, data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        mPacketID = ByteBuffer.wrap(data).getInt();

    }


    public int getPacketRequested(){
        return mPacketID;
    }
}
