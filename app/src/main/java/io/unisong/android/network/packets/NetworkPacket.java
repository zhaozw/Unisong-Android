package io.unisong.android.network.packets;

import java.net.DatagramPacket;

/**
 * Created by ezturner on 3/4/2015.
 */
public interface NetworkPacket {

    byte[] getData();

    int getPacketID();

    DatagramPacket getPacket();

}
