package io.unisong.android.network;

import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by ezturner on 2/13/2015.
 */
public class Host implements Serializable{

    private int port;
    private String phoneNumber;
    private InetAddress ip;
    private DatagramSocket socket;

    //The packets that are received while deciding which master to use
    private ArrayList<DatagramPacket> packets;

    public Host(int port, String number, InetAddress IP){
        this.port = port;
        phoneNumber = number;
        ip = IP;
        packets = new ArrayList<DatagramPacket>();
    }

    public void addPacket(DatagramPacket packet){
        packets.add(packet);
    }

    public ArrayList<DatagramPacket> getPackets(){
        return packets;
    }

    public int getPort(){
        return port;
    }

    public void setSocket(DatagramSocket socket){
        this.socket = socket;
    }

    public void closeSocket(){
        socket.close();
    }

    public String getPhoneNumber(){
        return phoneNumber;
    }

    public InetAddress getIP(){
        return ip;
    }

    public DatagramSocket getSocket(){
        return socket;
    }
}
