package com.ezturner.audiotracktest;

import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.ezturner.audiotracktest.network.AudioBroadcaster;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;

/**
 * Created by ezturner on 2/13/2015.
 */
public class DiscoveryHandler {


    private DatagramSocket mSocket;

    private Thread mListenerThread;
    private AudioBroadcaster mParent;

    public DiscoveryHandler(AudioBroadcaster parent){

        if(MediaService.isMulticast()){

        } else {
            try {
                mSocket = new DatagramSocket(AudioBroadcaster.DISCOVERY_PORT , AudioBroadcaster.getBroadcastAddress());
                mSocket.setBroadcast(true);
            } catch (Exception e) {
                Log.e("ezturner", e.toString());
            }
        }

        mListenerThread = startPacketListener();

        mListenerThread.start();
    }

    public DiscoveryHandler(MulticastSocket socket){

    }

    private Thread startPacketListener(){
        return new Thread(){
            public void run(){
                while(mParent.isStreamRunning()){
                    listenForPacket();
                }
            }
        };
    }

    //Listens for packets
    private void listenForPacket(){
        byte[] data = new byte[512];
        DatagramPacket packet = new DatagramPacket(data , data.length);

        try {
            mSocket.receive(packet);
        } catch(IOException e){
            Log.e("ezturner" , e.toString());
            return;
        }
        handlePacket(packet);


    }

    //Takes in a packet, and sends back the port in use and
    private void handlePacket(DatagramPacket packet){
        InetAddress addr = packet.getAddress();


        //Get port
        byte[] data = ByteBuffer.allocate(4).putInt(mParent.getPort()).array();

        //Get phone number
        byte[] number = MainActivity.getPhoneNumber().getBytes();

        //combine the two arrays
        data = AudioBroadcaster.combineArrays(data, number);

        //Make the packet
        DatagramPacket outPacket = new DatagramPacket(data , data.length , addr , AudioBroadcaster.DISCOVERY_PORT);

        //Send out packet
        try {
            mSocket.send(outPacket);
        } catch(IOException e){
            Log.e("ezturner" , e.toString());
        }
    }

}
