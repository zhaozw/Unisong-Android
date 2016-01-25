package io.unisong.android.network.ntp;


import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Ethan on 2/10/2015.
 */
public class UtpServer {

    public static final int UTP_PORT = 46232;

    private static final String LOG_TAG = UtpServer.class.getSimpleName();

    private static Thread mUtpListener;

    //boolean
    private static boolean mListening = false;

    //The socket for listening for NTP packets
    private static DatagramSocket mSocket;

    public UtpServer(){
        mListening = true;
        try {
            mSocket = new DatagramSocket(46232);
        } catch (IOException e){
            e.printStackTrace();
        }
        mUtpListener = startNtpListener();
        mUtpListener.start();
    }


    private Thread startNtpListener(){
        return new Thread(){
            public void run(){
                Log.d(LOG_TAG ,  "Starting to listen for NTP packets");
                while(mListening){
                    listenForNtpPackets();
                }
            }
        };
    }

    //Listens for NTP packets
    private void listenForNtpPackets(){
        byte[] data = new byte[UtpMessage.UTP_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(data , data.length);

        try {
            mSocket.receive(packet);
            handleUtpPacket(packet);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //Handles an incoming NTP packet
    private void handleUtpPacket(DatagramPacket packet){

        InetAddress address = packet.getAddress();

        byte[] data = packet.getData();
        UtpMessage message = new UtpMessage(data);


        message.setT2(System.currentTimeMillis());

        data = packet.getData();
        DatagramPacket responsePacket = new DatagramPacket(data ,0, data.length,address, UTP_PORT + 1);

        try {
            mSocket.send(responsePacket);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void stopUtpServer(){
        mListening = false;
        mSocket.close();
    }
}