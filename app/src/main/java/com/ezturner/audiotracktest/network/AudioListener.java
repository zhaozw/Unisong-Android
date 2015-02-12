package com.ezturner.audiotracktest.network;

import com.ezturner.audiotracktest.network.NTP.SntpClient;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.MulticastSocket;
import java.net.Socket;

/**
 * Created by Ethan on 2/8/2015.
 */
public class AudioListener {

    private SntpClient mSntpClient;
    private MulticastSocket mManagementSocket;

    //The listener for when a client requests a packet to be re-sent
    private Socket mReliabilitySocket;

    public AudioListener(String serverIP){
        mSntpClient = new SntpClient(serverIP);

        try {
            //forgive the lack of class constants, port 1731 4ever
            mManagementSocket = new MulticastSocket(1731);
            mManagementSocket.joinGroup(Inet4Address.getByName("238.17.0.29"));
        } catch(IOException e){
            e.printStackTrace();
        }

    }


    private void sendControlRequest(){

    }


}
