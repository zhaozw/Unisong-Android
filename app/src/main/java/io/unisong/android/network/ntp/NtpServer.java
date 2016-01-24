package io.unisong.android.network.ntp;


import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Ethan on 2/10/2015.
 */
public class NtpServer {

    private final int NTP_PORT = 46232;

    private static final String LOG_TAG = "NtpServer";

    private static final int NTP_PACKET_MAX_SIZE = 256;
    private static Thread mNtpListener;

    //boolean
    private static boolean mListening = false;

    //The socket for listening for NTP packets
    private static DatagramSocket mSocket;

    public NtpServer(){
        mListening = true;
        try {
            mSocket = new DatagramSocket(46232);
        } catch (IOException e){
            e.printStackTrace();
        }
        mNtpListener = startNtpListener();
        mNtpListener.start();
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
        byte[] data = new byte[NTP_PACKET_MAX_SIZE];
        DatagramPacket packet = new DatagramPacket(data , data.length);

        try {
            mSocket.receive(packet);
            handleNtpPacket(packet);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //Handles an incoming NTP packet
    private void handleNtpPacket(DatagramPacket packet){

        Log.d(LOG_TAG, "NTP packet received!");
        InetAddress address = packet.getAddress();
        byte[] buf = new NtpMessage().toByteArray();
        DatagramPacket responsePacket =
                new DatagramPacket(buf, buf.length, address,  46233);

        // Set the transmit timestamp *just* before sending the packet
        // ToDo: Does this actually improve performance or not?
        NtpMessage.encodeTimestamp(packet.getData(), 40,
                (System.currentTimeMillis() / 1000.0) + 2208988800.0);

        try {
            mSocket.send(responsePacket);
            Log.d(LOG_TAG , "NTP Response sent");
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void stopNtpServer(){
        mListening = false;
        mSocket.close();
    }
}