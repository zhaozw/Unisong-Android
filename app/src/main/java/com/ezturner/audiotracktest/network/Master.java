package com.ezturner.audiotracktest.network;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Created by ezturner on 2/13/2015.
 */
public class Master implements Serializable{

    private int mPort;
    private String mPhoneNumber;
    private InetAddress mIP;

    public Master(int port, String number, InetAddress IP){
        mPort = port;
        mPhoneNumber = number;
        mIP = IP;
    }

    public int getPort(){
        return mPort;
    }

    public String getPhoneNumber(){
        return mPhoneNumber;
    }

    public InetAddress getIP(){
        return mIP;
    }
}
