package com.ezturner.speakersync.network;

import com.ezturner.speakersync.network.ntp.SntpClient;

import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by ezturner on 2/13/2015.
 */
public class Master implements Serializable{

    private int mPort;
    private String mPhoneNumber;
    private InetAddress mIP;
    private DatagramSocket mSocket;

    //The packets that are received while deciding which master to use
    private ArrayList<DatagramPacket> mPackets;

    private SntpClient mClient;

    public Master(int port, String number, InetAddress IP ){
        mPort = port;
        mPhoneNumber = number;
        mIP = IP;
        mPackets = new ArrayList<DatagramPacket>();
    }

    public void addPacket(DatagramPacket packet){
        mPackets.add(packet);
    }

    public ArrayList<DatagramPacket> getPackets(){
        return mPackets;
    }

    public SntpClient getClient(){
        return mClient;
    }

    public void addClient(SntpClient client){
        mClient = client;
    }

    public int getPort(){
        return mPort;
    }

    public void setSocket(DatagramSocket socket){
        mSocket = socket;
    }

    public void closeSocket(){
        mSocket.close();
    }

    public String getPhoneNumber(){
        return mPhoneNumber;
    }

    public InetAddress getIP(){
        return mIP;
    }

    public DatagramSocket getSocket(){
        return mSocket;
    }
}
