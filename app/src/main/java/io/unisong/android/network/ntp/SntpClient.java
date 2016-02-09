package io.unisong.android.network.ntp;

import android.util.Log;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.xbill.DNS.Address;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;


/**
 * NtpClient - an NTP client for Java.  This program connects to an NTP server
 * and prints the response to the console.
 *
 * The local clock offset calculation is implemented according to the SNTP
 * algorithm specified in RFC 2030.
 *
 * Note that on windows platforms, the curent time-of-day timestamp is limited
 * to an resolution of 10ms and adversely affects the accuracy of the results.
 *
 *
 * This code is copyright (c) Adam Buckley 2004
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.  A HTML version of the GNU General Public License can be
 * seen at http://www.gnu.org/licenses/gpl.html
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * @author Adam Buckley
 */
public class SntpClient
{

    private final String LOG_TAG = SntpClient.class.getSimpleName();

    private static SntpClient instance;

    public static SntpClient getInstance(){
        return instance;
    }

    //The current server IP
    private final static String serverAddress = "pool.ntp.org";

    //The list of offsets to ensure that none of them are crazy innacurate
    private List<NtpEntry> results;

    //The offset, for everyone to see.
    private  double timeOffset;

    //The thread that all of the packets are sent/received on
    private Thread thread;

    //The socket.
    private DatagramSocket socket;

    private boolean stop;

    //The class that handles all of the time management stuff

    public SntpClient(){
        stop = false;
        timeOffset = 0;
        try {
            // Send request
            socket = new DatagramSocket();
        } catch(SocketException e){
            e.printStackTrace();
        }

        results = new ArrayList<>();
        Log.d(LOG_TAG , "SntpClient created");
        thread = getClientThread();
        thread.start();

        instance = this;
    }

    //Sends 4 different NTP packets and then calculates the average response time, removing outliers.
    public void sendPackets(){

        int total = 0;
        int count = 0;

        while(!stop){
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


            /*
            try{
                synchronized (this){
                    this.wait(1000);
                }
            } catch (InterruptedException iE){
//                    interruptedException.printStackTrace();
            }*/

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
        List<NtpEntry> entries = new ArrayList<>(results);

        for(int i = 0; i < results.size(); i++){
            stats.addValue(results.get(i).getOffset());
        }

        Log.d(LOG_TAG, "Unfiltered, size is : " + results.size());
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

        timeOffset = stats.getPercentile(50);

    }

    public long getOffset(){
        return (long) timeOffset;
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

        InetAddress address = Address.getByName(serverAddress);
        //Log.d(LOG_TAG , "address is " + address.toString());
        byte[] buf = new NtpMessage().toByteArray();

        DatagramPacket packet =
                new DatagramPacket(buf, buf.length, address, 123);

        // Set the transmit timestamp *just* before sending the packet
        // ToDo: Does this actually improve performance or not?
        NtpMessage.encodeTimestamp(packet.getData(), 40,
                (System.currentTimeMillis() / 1000.0) + 2208988800.0);

        socket.send(packet);


        // Get response
        //Log.d(LOG_TAG , "NTP request sent to : " + serverAddress +" , waiting for response...");
        packet = new DatagramPacket(buf, buf.length);
        // TODO : vary timeout with connection type, and prefer wifi/34/4g etc
        socket.setSoTimeout(750);
        try {
            socket.receive(packet);
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
            if(stop)
                return;
            getOneOffset();
            return;
        }

        // Immediately record the incoming timestamp
        double destinationTimestamp =
                (System.currentTimeMillis()/1000.0) + 2208988800.0;


        // Process response
        NtpMessage msg = new NtpMessage(packet.getData());

        // Corrected, according to RFC2030 errata
        double roundTripDelay = (destinationTimestamp-msg.originateTimestamp) -
                (msg.transmitTimestamp-msg.receiveTimestamp);

        double localClockOffset =
                ((msg.receiveTimestamp - msg.originateTimestamp) +
                        (msg.transmitTimestamp - destinationTimestamp)) / 2;


        // Display response

        /*
        Log.d(LOG_TAG , "NTP server: " + serverAddress);
        //Log.d(LOG_TAG , msg.toString());

        Log.d(LOG_TAG , "Dest. timestamp:     " +
                NtpMessage.timestampToString(destinationTimestamp));

        Log.d(LOG_TAG, "Round-trip delay: " +
                new DecimalFormat("0.00").format(roundTripDelay * 1000) + " ms");

        Log.d(LOG_TAG, "Local clock offset: " +
                new DecimalFormat("0.00").format(localClockOffset * 1000) + " ms");
        */
        NtpEntry entry = new NtpEntry(localClockOffset * 1000 , roundTripDelay * 1000);
        results.add(entry);
    }


    public void destroy(){
        stop = true;
        socket.close();
        socket = null;
    }



}