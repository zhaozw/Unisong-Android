package com.ezturner.speakersync.network.ntp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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

    //Listens for NTP packets
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

    //Handles an incoming NTP packet
    private static void handleNtpPacket(DatagramPacket packet){

        InetAddress address = packet.getAddress();
        byte[] buf = new com.ezturner.speakersync.network.ntp.NtpMessage().toByteArray();
        DatagramPacket responsePacket =
                new DatagramPacket(buf, buf.length, address, 123);

        // Set the transmit timestamp *just* before sending the packet
        // ToDo: Does this actually improve performance or not?
        com.ezturner.speakersync.network.ntp.NtpMessage.encodeTimestamp(packet.getData(), 40,
                (System.currentTimeMillis() / 1000.0) + 2208988800.0);

        try {
            mDatagramSocket.send(responsePacket);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void stopNtpServer(){
        mListening = false;
    }
}