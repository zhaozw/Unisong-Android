package com.ezturner.speakersync.network.packets;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by ezturner on 8/4/2015.
 */
public class DiscoveryPacket implements NetworkPacket {
    //The data for the packet that is being constructed
    private byte[] mData;

    private DatagramPacket mPacket;

    //TODO: flesh this out so that we can give masters a bit more info about clients.
    //The constructor for when you want to decode an incoming packet
    public DiscoveryPacket(byte[] data){
        mData = data;
        decode();
    }

    public DiscoveryPacket(){


        byte[] data = new byte[8];
        //combine the two arrays
        mData = data;//NetworkUtilities.combineArrays(data);

        mPacket = new DatagramPacket(mData , mData.length);
    }


    @Override
    public byte[] getData(){
        return mData;
    }

    private void decode(){

    }

    public int getPacketID(){
        return -1;
    }

    @Override
    public DatagramPacket getPacket() {
        return mPacket;
    }

    public String toString(){
        return "DiscoveryPacket";
    }
}
