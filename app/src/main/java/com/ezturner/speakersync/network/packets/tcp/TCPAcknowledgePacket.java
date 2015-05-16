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
public class TCPAcknowledgePacket {

    private static final String LOG_TAG = TCPAcknowledgePacket.class.getSimpleName();

    //The packet that has been acknowledged.
    private int mPacketID;

    public TCPAcknowledgePacket(InputStream stream){
        receive(stream);
    }

    public static void send(OutputStream stream , int packetID){
        byte[] data = ByteBuffer.allocate(4).putInt(packetID).array();


        synchronized (stream){
            try{
                stream.write(CONSTANTS.TCP_ACK);
                stream.write(data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void receive(InputStream stream){
        byte[] data = new byte[4];

        synchronized (stream){
            try{
                stream.read(data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        mPacketID = ByteBuffer.wrap(data).getInt();

    }

    public int getPacketAcknowledged(){
        return mPacketID;
    }
}
