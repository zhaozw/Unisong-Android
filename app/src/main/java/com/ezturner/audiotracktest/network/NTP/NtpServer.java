package com.ezturner.audiotracktest.network.NTP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by Ethan on 2/10/2015.
 */
public class NtpServer {

    private static final int NTP_PACKET_MAX_SIZE = 256;
    private static Thread mNtpListener;

    //boolean
    private static boolean mListening = false;

    //The socket for listening for NTP packets
    private static DatagramSocket mDatagramSocket;

    public static void startNtpServer(){
        mListening = true;
        try {
            mDatagramSocket = new DatagramSocket();
        } catch (IOException e){
            e.printStackTrace();
        }
        mNtpListener = startNtpListener();

    }

    private static Thread startNtpListener(){
        return new Thread(){
            public void run(){
                while(mListening){
                    listenForNtpPackets();
                }
            }
        };
    }

    private static void listenForNtpPackets(){
        byte[] data = new byte[NTP_PACKET_MAX_SIZE];
        DatagramPacket packet = new DatagramPacket(data , 2048);

        try {
            mDatagramSocket.receive(packet);
            handleNtpPacket(packet);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private static void handleNtpPacket(DatagramPacket packet){

    }

    public static void stopNtpServer(){
        mListening = false;
    }
}
