package io.unisong.android.network.packets.tcp;

import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.NetworkUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ezturner on 5/5/2015.
 */
public class TCPSwitchMasterPacket {

    //The new Master's IP address
    private String mIP;

    public TCPSwitchMasterPacket(InputStream stream){
        receive(stream);
    }

    public static void send(OutputStream stream, String IP){
        if(IP.contains("/")){
            IP = IP.substring(1);
        }

        String[] bytes = IP.split(".");

        byte[] address = new byte[4];

        for(int i = 0; i < bytes.length && i < address.length; i++){
            address[i] = Byte.decode(bytes[i]);
        }


        synchronized (stream) {
            try {
                stream.write(CONSTANTS.TCP_SWITCH_MASTER);
                stream.write(address);
                stream.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

    }


    private void receive(InputStream stream){
        byte[] address= new byte[4];


        synchronized (stream) {
            try {
                NetworkUtilities.readFromStream(stream, address);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        mIP = address[0] + "." + address[1] + "." + address[2] + "." + address[3];
    }

}
