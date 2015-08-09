package com.ezturner.speakersync.network.packets.tcp;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.Client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The TCP packet that sends client information
 * Created by Ethan on 8/9/2015.
 */
public class TCPClientInfoPacket {

    private Client mClient;

    public TCPClientInfoPacket(InputStream stream){
        receive(stream);
    }

    public static void send(OutputStream stream, Client client) {
        byte[] data = client.getBytes();

        synchronized (stream) {
            try {
                stream.write(CONSTANTS.TCP_CLIENT_INFO);
                stream.write(data.length);
                stream.write(data);
                stream.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void receive(InputStream stream){

        synchronized (stream){
            mClient = new Client(stream);
        }

    }

    public Client getClient(){
        return mClient;
    }
}
