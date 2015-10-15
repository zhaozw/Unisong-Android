package io.unisong.android.network.ntp;

/**
 * Created by Ethan on 10/14/2015.
 */
public class NtpEntry {

    private double mOffset;
    private double mLatency;

    public NtpEntry(double offset, double latency){
        mOffset = offset;
        mLatency = latency;
    }

    public double getLatency(){
        return mLatency;
    }

    public double getOffset(){
        return mOffset;
    }
}
