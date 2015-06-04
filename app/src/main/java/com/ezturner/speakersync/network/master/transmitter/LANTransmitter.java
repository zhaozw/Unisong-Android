package com.ezturner.speakersync.network.master.transmitter;

import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;
import com.ezturner.speakersync.network.master.MasterDiscoveryHandler;
import com.ezturner.speakersync.network.master.MasterFECHandler;
import com.ezturner.speakersync.network.master.MasterTCPHandler;
import com.ezturner.speakersync.network.packets.NetworkPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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

    Runnable mPacketSender = new Runnable() {
        @Override
        public void run() {


            mSendRunnableRunning = true;
            long begin = System.currentTimeMillis();

//            Log.d(LOG_TAG , "Starting packet send!");
            NetworkPacket packet;
            AudioFrame frame;

            //Wait for mFrames to co
            while (!mFrames.containsKey(mNextFrameSendID)){
//                Log.d(LOG_TAG , "Frame " + mNextFrameSendID + " not found! mFrames size is :" + mFrames.size());
                synchronized (this){
                    try {
                        this.wait(1);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }

            synchronized (mFrames) {
                frame = mFrames.get(mNextFrameSendID);
            }

            if(mNextFrameSendID == 4306)    Log.d(LOG_TAG, frame.toString());
            packet = createFramePacket(frame);
            if(packet == null){
                Log.d(LOG_TAG , "Packet #" + mNextFrameSendID + " is null! AudioFrame is : " + frame);

            }


            mNextFrameSendID++;



//            Log.d(LOG_TAG , "Sending packet!");
            synchronized (mStreamSocket){
                try {
                    DatagramPacket datagramPacket = packet.getPacket();

                    if(datagramPacket == null){
                        Log.d(LOG_TAG , "The datagram packet is null for packet #" + (mNextFrameSendID -1));
                    }

                    if(!mStreamRunning){
                        return;
                    }
                    mStreamSocket.send(datagramPacket);

                    mPacketsSentCount++;
                } catch (IOException e){
                    e.printStackTrace();
                }
            }

//
//            Log.d(LOG_TAG , "Packet Sent!");
            long delay = getDelay();


            rebroadcast();
            delay -= System.currentTimeMillis() - begin;

            if(delay < 0){
                delay = 0;
            }

            long diff = System.currentTimeMillis() - mTimeManager.getAACPlayTime(packet.getPacketID());
//            Log.d(LOG_TAG , "For Packet #" + packet.getPacketID() + " , the difference between now and play time is : " + diff + "ms" );
            if(mPacketsSentCount % 100 == 0) {

                Log.d(LOG_TAG, "mPacketsSentCount :" + mPacketsSentCount + " , delay is : " + delay);
            }


            if(mNextFrameSendID != mLastFrameID || !mEncodeDone && mIsBroadcasting) {
                mWorker.schedule(mPacketSender , delay , TimeUnit.MILLISECONDS);
            }
            mSendRunnableRunning = false;
        }

    };

    private void startSong(){
        mStreamSocket.connect(mStreamIP , getPort());
    }

    public int getPort(){
        return mPort;
    }

}
