package com.ezturner.speakersync.network.master.transmitter;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.master.MasterDiscoveryHandler;
import com.ezturner.speakersync.network.master.MasterFECHandler;
import com.ezturner.speakersync.network.master.MasterTCPHandler;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/**
 * This class handles the Broadcast/Multicast functionality
 * Created by Ethan on 5/22/2015.
 */
public class LANTransmitter {

    //The port the stream will run on
    private int mPort;

    //Random object, used to randomize multicast stream IP
    static private Random random = new Random();

    //The FEC Handler
    private MasterFECHandler mMasterFECHandler;

    //The IP that the broadcast stream will be sent on
    private InetAddress mStreamIP;

    //The multicast listener for giving out the IP of the multicast stream
    private DatagramSocket mStreamSocket;

    //Handles the network discovery
    private MasterDiscoveryHandler mDiscoveryHandler;

    //The object that handles all reliability stuff
    private MasterTCPHandler mTCPHandler;

    public LANTransmitter(boolean multicast){
        mPort = CONSTANTS.STREAM_PORT_BASE + random.nextInt(CONSTANTS.PORT_RANGE);
        //TODO: Listen for other streams and ensure that you don't use the same port

        mMasterFECHandler = new MasterFECHandler();

        try {
            mStreamIP = NetworkUtilities.getBroadcastAddress();
            //Start the socket for the actual stream
            mStreamSocket = new DatagramSocket();

            mDiscoveryHandler = new MasterDiscoveryHandler(this);
            mTCPHandler = new MasterTCPHandler(this);

        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
