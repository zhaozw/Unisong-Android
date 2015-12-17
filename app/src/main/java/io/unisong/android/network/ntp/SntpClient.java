package io.unisong.android.network.ntp;

import android.util.Log;

import org.xbill.DNS.Address;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
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

    private static SntpClient sInstance;

    public static SntpClient getInstance(){
        return sInstance;
    }

    //The current server IP
    private final static String sServerAddress = "pool.ntp.org";

    //The list of offsets to ensure that none of them are crazy innacurate
    private List<NtpEntry> mResults;

    //The offset, for everyone to see.
    private  double mTimeOffset;

    //The thread that all of the packets are sent/received on
    private Thread mThread;

    //The socket.
    private DatagramSocket mSocket;

    private boolean mDestroyed;

    //The class that handles all of the time management stuff

    public SntpClient(){
        mDestroyed = false;
        mTimeOffset = 0;
        try {
            // Send request
            mSocket = new DatagramSocket();
        } catch(SocketException e){
            e.printStackTrace();
        }

        mResults = new ArrayList<>();
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
                } catch (InterruptedException interruptedException){
                    interruptedException.printStackTrace();
                }
                continue;
            }

            if(count > 20 && total < 25){
                count = 0;
                calculateOffset();
            } else if(count > 50 && total < 200){
                count = 0;
                calculateOffset();
            } else if(count > 100){
                calculateOffset();
            }

            if(total >= 600){
                return;
            }
        }
    }

    private void calculateOffset(){
        double offset = 0;

        for(int i = 0; i < mResults.size(); i++){
            offset += mResults.get(i).getOffset();
        }

        offset = offset / mResults.size();

        mTimeOffset = offset;
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

        InetAddress address = Address.getByName(sServerAddress);
        Log.d(LOG_TAG , "address is " + address.toString());
        byte[] buf = new NtpMessage().toByteArray();
        //TODO: change this to NTP_PORT
        DatagramPacket packet =
                new DatagramPacket(buf, buf.length, address, 123);

        // Set the transmit timestamp *just* before sending the packet
        // ToDo: Does this actually improve performance or not?
        NtpMessage.encodeTimestamp(packet.getData(), 40,
                (System.currentTimeMillis() / 1000.0) + 2208988800.0);

        mSocket.send(packet);


        // Get response
        Log.d(LOG_TAG , "NTP request sent to : " + sServerAddress +" , waiting for response...");
        packet = new DatagramPacket(buf, buf.length);
        mSocket.setSoTimeout(500);
        try {
            mSocket.receive(packet);
        } catch (SocketTimeoutException e){
            Log.d(LOG_TAG , e.toString());
            // resend
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


        Log.d(LOG_TAG , "NTP server: " + sServerAddress);
        //Log.d(LOG_TAG , msg.toString());

        Log.d(LOG_TAG , "Dest. timestamp:     " +
                NtpMessage.timestampToString(destinationTimestamp));

        Log.d(LOG_TAG, "Round-trip delay: " +
                new DecimalFormat("0.00").format(roundTripDelay * 1000) + " ms");

        Log.d(LOG_TAG, "Local clock offset: " +
                new DecimalFormat("0.00").format(localClockOffset * 1000) + " ms");

        NtpEntry entry = new NtpEntry(localClockOffset * 1000 , roundTripDelay * 1000);
        mResults.add(entry);
    }


    public void destroy(){
        mDestroyed = true;
        mSocket.close();
        mSocket = null;
    }



}