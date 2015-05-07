package com.ezturner.speakersync.network.packets.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ezturner on 5/5/2015.
 */
public class TCPSwitchMasterPacket {

    //The new Master's IP address
    private String mIP;

    public TCPSwitchMasterPacket(InputStream stream) throws IOException{
        receive(stream);
    }

    public static void send(OutputStream stream, String IP) throws IOException{
        if(IP.contains("/")){
            IP = IP.substring(1);
        }

        String[] bytes = IP.split(".");

        byte[] address = new byte[4];

        for(int i = 0; i < bytes.length && i < address.length; i++){
            address[i] = Byte.decode(bytes[i]);
        }

        stream.write(address);

    }


    private void receive(InputStream stream) throws IOException{
        byte[] address= new byte[4];

        stream.read(address);

        mIP = address[0] + "." + address[1] + "." + address[2] + "." + address[3];
    }

}
