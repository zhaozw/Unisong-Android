package com.ezturner.speakersync.network.packets.tcp;

import com.ezturner.speakersync.network.CONSTANTS;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by ezturner on 5/18/2015.
 */
public class TCPMasterClosePacket {


    public static void send(OutputStream stream){
        synchronized (stream){
            try{
                stream.write(CONSTANTS.TCP_MASTER_CLOSE);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
