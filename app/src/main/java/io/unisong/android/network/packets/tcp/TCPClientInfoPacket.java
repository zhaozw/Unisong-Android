package io.unisong.android.network.packets.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.Client;

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
