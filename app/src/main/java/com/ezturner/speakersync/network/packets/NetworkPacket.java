package com.ezturner.speakersync.network.packets;

import java.net.DatagramPacket;

/**
 * Created by ezturner on 3/4/2015.
 */
public interface NetworkPacket {

    public byte[] getData();

    public byte getStreamID();

    public int getPacketID();

    public DatagramPacket getPacket();

}
