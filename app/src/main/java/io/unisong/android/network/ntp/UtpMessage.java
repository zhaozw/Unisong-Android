package io.unisong.android.network.ntp;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

import io.unisong.android.network.NetworkUtilities;

/**
 * A message for unisong-time-protocol, basically NTP but using
 * UTC time without any conversions.
 *
 * Refer to this wikipedia article for details:
 *
 * Created by ezturner on 1/25/2016.
 */
public class UtpMessage {

    public static final int UTP_PACKET_SIZE = 33;
    private static final String LOG_TAG = UtpMessage.class.getSimpleName();
    private boolean mIsReply;

    // The time the packet was originally sent
    private long mT0;

    // the time the packet was received on the server
    private long mT1;

    // the time the packet was sent on the server
    private long mT2;

    // the time the packet was received on the client
    private long mT3;

    /**
     * Creates a UTPMessage and prepares it for send.
     */
    public UtpMessage(){
        mIsReply = false;
        mT0 = System.currentTimeMillis();
    }

    /**
     * Creates a UTPMessage from the wire
     */
    public UtpMessage(byte[] data){
        decodeByteArray(data);
        mIsReply = true;
    }

    public byte[] getData(){
        return toByteArray();
    }

    private byte[] toByteArray(){
        byte[] data = new byte[1];
        data[0] = (byte) (mIsReply ? 1 : 0);

        byte[] t0 = ByteBuffer.allocate(8).putLong(mT0).array();
        byte[] t1 = ByteBuffer.allocate(8).putLong(mT1).array();
        byte[] t2 = ByteBuffer.allocate(8).putLong(mT2).array();
        byte[] t3 = ByteBuffer.allocate(8).putLong(mT3).array();

        data = NetworkUtilities.combineArrays(data, t0);
        data = NetworkUtilities.combineArrays(data, t1);
        data = NetworkUtilities.combineArrays(data, t2);
        data = NetworkUtilities.combineArrays(data, t3);

        Log.d(LOG_TAG, "UtpMessage size is : " + data.length);

        return data;
    }

    private void decodeByteArray(byte[] data){
        mIsReply = data[0] == 1;

        byte[] t0 = Arrays.copyOfRange(data , 1 , 9);
        byte[] t1 = Arrays.copyOfRange(data , 9 , 17);
        byte[] t2 = Arrays.copyOfRange(data , 17 , 25);
        byte[] t3 = Arrays.copyOfRange(data , 25 , 33);

        mT0 = ByteBuffer.wrap(t0).getLong();
        mT1 = ByteBuffer.wrap(t1).getLong();
        mT2 = ByteBuffer.wrap(t2).getLong();
        mT3 = ByteBuffer.wrap(t3).getLong();

        if(!mIsReply)
            mT1 = System.currentTimeMillis();

        mIsReply = !mIsReply;
    }

    public double getOffset(){
        return ((mT1 - mT0) + (mT2 - mT3) / 2);
    }

    public double getLatency(){
        return (mT3 - mT0) - (mT2 - mT1);
    }

    public void setT1(long t1){
        mT1 = t1;
    }

    public void setT2(long t2){
        mT2 = t2;
    }

    public boolean isReply(){
        return mIsReply;
    }

}
