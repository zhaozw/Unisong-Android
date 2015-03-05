package com.ezturner.speakersync.network;

import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by ezturner on 2/13/2015.
 */
public class Master implements Serializable{

    private int mPort;
    private String mPhoneNumber;
    private InetAddress mIP;
    private DatagramSocket mSocket;

    public Master(int port, String number, InetAddress IP ){
        mPort = port;
        mPhoneNumber = number;
        mIP = IP;
    }

    public int getPort(){
        return mPort;
    }

    public void setSocket(DatagramSocket socket){
        mSocket = socket;
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
