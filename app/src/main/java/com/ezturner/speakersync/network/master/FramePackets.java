package com.ezturner.speakersync.network.master;

import com.ezturner.speakersync.network.packets.FrameDataPacket;
import com.ezturner.speakersync.network.packets.FrameInfoPacket;
import com.ezturner.speakersync.network.packets.NetworkPacket;

import java.util.ArrayList;

/**
 * Created by Ethan on 3/12/2015.
 */
public class FramePackets {

    private FrameInfoPacket mInfoPacket;
    private ArrayList<FrameDataPacket> mDataPackets;

    public FramePackets(FrameInfoPacket infoPacket , ArrayList<FrameDataPacket> dataPackets){
        mInfoPacket = infoPacket;
        mDataPackets = dataPackets;
    }

    public FrameInfoPacket getInfoPacket(){
        return mInfoPacket;
    }

    public ArrayList<FrameDataPacket> getDataPackets(){
        return mDataPackets;
    }

    public ArrayList<NetworkPacket> getPackets(){
        ArrayList<NetworkPacket> packets = new ArrayList<NetworkPacket>();

        packets.add(mInfoPacket);

        for(int i = 0; i < mDataPackets.size(); i++){
            packets.add(mDataPackets.get(i));
        }

        return packets;
    }
}
