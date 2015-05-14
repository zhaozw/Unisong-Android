package com.ezturner.speakersync.network;

import com.ezturner.speakersync.network.master.Slave;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ezturner on 5/13/2015.
 */
public class AnalyticsSuite {

    //The time that all packets were sent
    private Map<Integer , Long> mPacketSendTimes;
    private Map<Slave , Map<Integer, Long>> mAckDelayTimes;
    private int mPacketsRequested = 0;

    public AnalyticsSuite(){
        mPacketSendTimes = new HashMap<>();
        mAckDelayTimes = new HashMap<>();

    }

    public void packetSent(int packetID){
        mPacketSendTimes.put(packetID, System.currentTimeMillis());
    }

    public void ackReceived(int packetID , Slave slave){

        Map<Integer, Long> ackDelayMap = null;

        long diff = System.currentTimeMillis() - mPacketSendTimes.get(packetID);

        if(mAckDelayTimes.containsKey(slave)){
            ackDelayMap = mAckDelayTimes.get(slave);
            ackDelayMap.put(packetID , diff);

        } else {
            ackDelayMap = new HashMap<>();
            mAckDelayTimes.put(slave , ackDelayMap);
            ackDelayMap.put(packetID , diff);

        }
    }

    public void packetRequested(){
        mPacketsRequested++;
    }
}
