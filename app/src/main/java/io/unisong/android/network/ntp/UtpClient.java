package io.unisong.android.network.ntp;

import android.os.Build;
import android.util.Log;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.xbill.DNS.Address;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * UtpClient - a Client for UTP
 */
public class UtpClient
{

    private final String LOG_TAG = UtpClient.class.getSimpleName();

    private static UtpClient sInstance;

    public static UtpClient getInstance(){
        return sInstance;
    }

    //The current server IP
    private final static String sServerAddress = "pool.ntp.org";

    //The list of offsets to ensure that none of them are crazy innacurate
    private List<UtpMessage> mResults;

    //The offset, for everyone to see.
    private  double mTimeOffset;

    //The thread that all of the packets are sent/received on
    private Thread mThread;

    //The socket.
    private DatagramSocket mSocket;

    private boolean mDestroyed;

    //The class that handles all of the time management stuff

    public UtpClient(){
        mDestroyed = false;
        mTimeOffset = 0;
        try {
            // Send request
            mSocket = new DatagramSocket(UtpServer.UTP_PORT + 1);
        } catch(SocketException e){
            e.printStackTrace();
        }

        mResults = new ArrayList<>();
        Log.d(LOG_TAG , "UtpClient created");
        mThread = getClientThread();
        mThread.start();

        sInstance = this;
    }

    //Sends 4 different NTP packets and then calculates the average response time, removing outliers.
    public void sendPackets(){

        int total = 0;
        int count = 0;

        while(!mDestroyed){
            count++;
            total++;

            try {
                getOneOffset();
            } catch (IOException e){
                // If we're having an IOException wait 200ms and then try again
                e.printStackTrace();
                try{
                    synchronized (this){
                        this.wait(200);
                    }
                } catch (InterruptedException iE){
//                    interruptedException.printStackTrace();
                }
                continue;
            }

            if(total == 10){
                calculateOffset();
            }

            if(total % 50 == 0){
                calculateOffset();
            }

            if(total >= 200){
                calculateOffset();
                return;
            }
        }
    }

    private void calculateOffset(){

        // TODO: figure out a way to create a method here?
        // note : i think making a method here would be counterproductive
        // because using reflection would take longer than it would save
        DescriptiveStatistics stats = new DescriptiveStatistics();
        List<UtpMessage> entries = new ArrayList<>(mResults);

        for(int i = 0; i < mResults.size(); i++){
            stats.addValue(mResults.get(i).getOffset());
        }

        Log.d(LOG_TAG , "Unfiltered, size is : " + mResults.size());
        Log.d(LOG_TAG, "Mean is: " + stats.getMean());
        Log.d(LOG_TAG , "Standard Deviation is : " + stats.getStandardDeviation());
        Log.d(LOG_TAG, "Median is : " + stats.getPercentile(50));


        // Inter-quartile range
        double IQR = stats.getPercentile(75) - stats.getPercentile(25);
        double lowerOutlierBound = stats.getPercentile(25) - IQR;
        double higherOutlierBount = stats.getPercentile(75) - IQR;

        for(int i = 0; i < entries.size(); i++){
            // If we are an outlier, remove it
            if(entries.get(i).getOffset() <= lowerOutlierBound ||
                    entries.get(i).getOffset() <= higherOutlierBount ){
                entries.remove(i);
                i--;
            }
        }

        stats = new DescriptiveStatistics();

        for(int i = 0; i < entries.size(); i++){
            stats.addValue(entries.get(i).getLatency());
        }
        Log.d(LOG_TAG , "Average latency : " + stats.getMean() + " , median : " + stats.getPercentile(50));

        // Inter-quartile range
        IQR = stats.getPercentile(75) - stats.getPercentile(25);
        lowerOutlierBound = stats.getPercentile(25) - IQR;
        higherOutlierBount = stats.getPercentile(75) - IQR;

        for(int i = 0; i < entries.size(); i++){
            // If we are an outlier, remove it
            if(entries.get(i).getLatency() <= lowerOutlierBound ||
                    entries.get(i).getLatency() <= higherOutlierBount ){
                entries.remove(i);
                i--;
            }
        }


        stats = new DescriptiveStatistics();
        for(int i = 0; i < entries.size(); i++){
            stats.addValue(entries.get(i).getOffset());
        }

        Log.d(LOG_TAG , "Filtering done, current size is: " + entries.size());
        Log.d(LOG_TAG, "Mean is: " + stats.getMean());
        Log.d(LOG_TAG , "Standard Deviation is : " + stats.getStandardDeviation());
        Log.d(LOG_TAG, "Median is : " + stats.getPercentile(50));

        mTimeOffset = stats.getPercentile(50);

    }

    public long getOffset(){
        return (long)mTimeOffset;
    }

    private Thread getClientThread(){
        return new Thread(){
            public void run(){
                sendPackets();
            }
        };
    }

    //Currently unused
    private Thread waitForRecheck(){
        return new Thread(){
            public void run(){
                try {
                    Thread.sleep(15 * 60 * 1000);
                } catch (InterruptedException e){
                    Log.d(LOG_TAG, "NTP Re-Check wait interrupted!");
                }
            }
        };
    }


    private void getOneOffset() throws IOException{
        // Send request

        InetAddress address;
        if(Build.MODEL.equals("SGH999")){
            address = Address.getByName("128.95.62.170");
        } else {
            address = Address.getByName("140.142.143.195");
        }
        //Log.d(LOG_TAG , "address is " + address.toString());
        byte[] buf = new UtpMessage().getData();

        DatagramPacket packet =
                new DatagramPacket(buf, buf.length, address, UtpServer.UTP_PORT);

        mSocket.send(packet);


        // Get response
        //Log.d(LOG_TAG , "NTP request sent to : " + sServerAddress +" , waiting for response...");
        packet = new DatagramPacket(buf, buf.length);
        // TODO : vary timeout with connection type, and prefer wifi/34/4g etc
        mSocket.setSoTimeout(750);
        try {
            mSocket.receive(packet);
        } catch (SocketTimeoutException e){
            try{
                synchronized (this){
                    this.wait(1000);
                }
            } catch (InterruptedException iE){
//                    interruptedException.printStackTrace();
            }

//            Log.d(LOG_TAG , e.toString());
            // resend
            // TODO : ensure we don't get a stack overflow here
            getOneOffset();
            return;
        } catch (IOException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "UtpClient had an IOException!");
            return;
        }


        UtpMessage entry = new UtpMessage(packet.getData());
        mResults.add(entry);
    }


    public void destroy(){
        mDestroyed = true;
        mSocket.close();
        mSocket = null;
    }



}