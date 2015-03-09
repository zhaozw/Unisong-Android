package com.ezturner.speakersync.network.ntp;

import android.util.Log;

import com.ezturner.speakersync.audio.AudioTrackManager;
import com.ezturner.speakersync.network.slave.AudioListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;


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

    private final int NTP_PORT = 46232;

    private final String LOG_TAG = "SntpClient";

    //The current server IP
    private String mServerIP = "";

    //The list of offsets to ensure that none of them are crazy innacurate
    private  ArrayList<Double> list = new ArrayList<Double>();

    //The offset, for everyone to see.
    private  double mTimeOffset;

    //Set to check if the calculation is done.
    private int mNumberDone;

    //The thread that all of the packets are sent/received on
    private Thread mThread;

    //The socket.
    private DatagramSocket mSocket;

    //The audio track manager that needs to get the clock offset
    private AudioListener mParent;

    public SntpClient(String serverIP , AudioListener parent){
        mServerIP = serverIP;
        try {
            // Send request
            mSocket = new DatagramSocket(46232);
        } catch(SocketException e){
            e.printStackTrace();
        }

        mParent = parent;

        mThread = getClientThread();
        mThread.start();

    }

    //Sends 4 different NTP packets and then calculates the average response time, removing outliers.
    public double calculateOffset() throws IOException{

        mNumberDone = 0;

        for(int i = 0; i < 4; i++){
            getOneOffset();/*
            try {
                wait(5);
            } catch(InterruptedException e){
                e.printStackTrace();
            }*/
        }

        mSocket.close();/*
        while(mNumberDone < 4){
            try {
                wait();
            } catch(InterruptedException e){
                e.printStackTrace();
            }
        }*/

        mNumberDone = 0;

        double average = 0;

        for(int i = 0; i < list.size(); i++){
            average += list.get(i);
        }

        average = average / list.size();

        mTimeOffset = average;
        list = new ArrayList<Double>();

        return average;
    }

    public double getOffset(){
        return mTimeOffset;
    }

    private Thread getClientThread(){
        return new Thread(){
            public void run(){
                try {
                    mTimeOffset = calculateOffset();
                    mParent.setOffset(mTimeOffset);
                } catch(IOException e){
                    e.printStackTrace();
                }
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
                    Log.d(LOG_TAG , "NTP Re-Check wait interrupted!");
                }
            }
        };
    }

    private void getOneOffset() throws IOException{

        // Send request
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName(mServerIP.substring(1));
        byte[] buf = new com.ezturner.speakersync.network.ntp.NtpMessage().toByteArray();
        DatagramPacket packet =
                new DatagramPacket(buf, buf.length, address, NTP_PORT);

        // Set the transmit timestamp *just* before sending the packet
        // ToDo: Does this actually improve performance or not?
        com.ezturner.speakersync.network.ntp.NtpMessage.encodeTimestamp(packet.getData(), 40,
                (System.currentTimeMillis() / 1000.0) + 2208988800.0);

        socket.send(packet);


        // Get response
        Log.d(LOG_TAG , "NTP request sent, waiting for response...\n");
        packet = new DatagramPacket(buf, buf.length);
        socket.setSoTimeout(100);
        try {
            socket.receive(packet);
        } catch (SocketTimeoutException e) {
            // resend
            mNumberDone++;
            return;
        }

        // Immediately record the incoming timestamp
        double destinationTimestamp =
                (System.currentTimeMillis()/1000.0) + 2208988800.0;


        // Process response
        com.ezturner.speakersync.network.ntp.NtpMessage msg = new com.ezturner.speakersync.network.ntp.NtpMessage(packet.getData());

        // Corrected, according to RFC2030 errata
        double roundTripDelay = (destinationTimestamp-msg.originateTimestamp) -
                (msg.transmitTimestamp-msg.receiveTimestamp);

        double localClockOffset =
                ((msg.receiveTimestamp - msg.originateTimestamp) +
                        (msg.transmitTimestamp - destinationTimestamp)) / 2;


        // Display response
        Log.d(LOG_TAG , "NTP server: " + mServerIP);
        Log.d(LOG_TAG , msg.toString());

        Log.d(LOG_TAG , "Dest. timestamp:     " +
                com.ezturner.speakersync.network.ntp.NtpMessage.timestampToString(destinationTimestamp));

        Log.d(LOG_TAG , "Round-trip delay: " +
                new DecimalFormat("0.00").format(roundTripDelay*1000) + " ms");

        Log.d(LOG_TAG , "Local clock offset: " +
                new DecimalFormat("0.00").format(localClockOffset*1000) + " ms");

        socket.close();

        list.add(localClockOffset * 1000);
        mNumberDone++;
    }





}