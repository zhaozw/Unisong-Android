package com.ezturner.speakersync.network.master;

import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.AnalyticsSuite;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.packets.tcp.TCPAcknowledgePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPFramePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPLastFramePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPPausePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPRequestPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPResumePacket;
import com.ezturner.speakersync.network.packets.tcp.TCPRetransmitPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPSeekPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPSongInProgressPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPSongStartPacket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Ethan on 2/11/2015.
 */
public class MasterTCPHandler {

    private String LOG_TAG = MasterTCPHandler.class.getSimpleName();

    //The listener for when a client joins the session and starts a TCP handshake
    private ServerSocket mServerSocket;

    //The list of all of the TCP control sockets that are listening for reliability packets
    private Map<Slave ,Socket> mSockets;

    //The list of all of the DataOutputStreams by slave
    private Map<Slave , DataOutputStream> mOutputStreams;

    //The list of all of the DataInputStreams by slave
    private Map<Slave , DataInputStream> mInputStreams;

    //The AudioBroadcaster that this class interfaces with
    private AudioBroadcaster mBroadcaster;

    //The thread that'll listen to reliability packets
    private Thread mServerSocketThread;

    private List<Thread> mSocketThreads;

    private boolean mRunning;

    //The random number generator for choosing which slave to have as a rebroadcaster
    private Random mRandom;
    //TODO: Implement passive listening using AudioBroadcaster.DISOVERY_PASSIVE_PORT

    private AnalyticsSuite mAnalyticsSuite;

    public MasterTCPHandler(AudioBroadcaster broadcaster, AnalyticsSuite analyticsSuite){

        mAnalyticsSuite = analyticsSuite;

        mBroadcaster = broadcaster;

        mInputStreams = new HashMap<>();
        mOutputStreams = new HashMap<>();
        mRecentlyRebroadcasted = new HashMap<>();

        mRandom = new Random();

        try {
            
            mServerSocket = new ServerSocket(CONSTANTS.RELIABILITY_PORT);

        } catch(IOException e){
            e.printStackTrace();
        }

        mSockets = new HashMap<>();
        mSocketThreads = new ArrayList<>();

        mServerSocketThread = startReliabilityConnectionListener();

        mRunning = true;
        mServerSocketThread.start();
    }

    //Starts the listener for new connections
    private Thread startReliabilityConnectionListener(){
        return new Thread(){
            public void run(){
                Log.d(LOG_TAG , "Starting to listen for sockets");
                while(mRunning){
                    Socket socket = null;

                    try {
                        socket = mServerSocket.accept();
                    } catch(IOException e){
                        e.printStackTrace();
                    }
                    //TODO: uncomment after you
//                    if(socket == null){
//                        break;
//                    }
                    Log.d(LOG_TAG , "Socket connected : " + socket.getInetAddress());

                    if(socket != null){

                        try {
                            DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                            DataInputStream inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                            Thread thread = startSocketListener(inputStream , outputStream , socket);
                            mSocketThreads.add(thread);
                            thread.start();
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    //Start listening for packets
    private Thread startSocketListener(DataInputStream inStream , DataOutputStream outStream, Socket socket){
        class SocketRunnable implements Runnable {
            DataInputStream inStream;
            DataOutputStream outStream;
            Socket socket;
            SocketRunnable(DataInputStream inputStream , DataOutputStream outputStream , Socket ssocket) {
                inStream = inputStream;
                outStream = outputStream;
                socket = ssocket;
            }
            public void run(){
                try {
                    listenToReliabilitySocket(inStream , outStream , socket);
                } catch(IOException e){
                    e.printStackTrace();
                }
            }
        };

        return new Thread(new SocketRunnable(inStream , outStream , socket));
    }

    //Handle new data coming in from a Reliability socket
    private void listenToReliabilitySocket(DataInputStream inputStream , DataOutputStream outputStream ,Socket socket) throws IOException{
        //TODO: see if we need to get rid of the port for this to work
        Slave slave = new Slave(socket.getRemoteSocketAddress().toString());

        mBroadcaster.addSlave(slave);
        mSockets.put(slave, socket);
        mInputStreams.put(slave, inputStream);
        mOutputStreams.put(slave , outputStream);


        if(mBroadcaster.isStreamRunning()) {
            sendSongInProgress(slave);
        }

        byte identifier;
        synchronized (inputStream) {
             identifier= inputStream.readByte();
        }
        while(identifier != -1 && mRunning){
            handleDataReceived(identifier, slave , inputStream);
            synchronized (inputStream) {
                identifier = inputStream.readByte();
            }
        }

        if(socket.isClosed()){
            mBroadcaster.removeSlave(slave);
        }
    }


    //Hanldes the identifying byte and redirects it to the right method
    private void handleDataReceived(int identifier , Slave slave , DataInputStream inputStream){
        switch (identifier){
            case CONSTANTS.TCP_REQUEST:
                int packetID = new TCPRequestPacket(inputStream).getPacketRequested();
                if(packetID != -1 || !checkSlaves(packetID))  mBroadcaster.rebroadcastPacket(packetID);
                break;
            case CONSTANTS.TCP_ACK:
                int ID = new TCPAcknowledgePacket(inputStream).getPacketAcknowledged();
                mAnalyticsSuite.ackReceived(ID , slave);
                if(ID != -1)    slave.packetReceived(ID);
                break;
        }
    }

    private Map<Integer, Long> mRecentlyRebroadcasted;
    //Checks to see if any of the slaves have the packet in question.
    public boolean checkSlaves(int packetID){
        List<Slave> slaves = mBroadcaster.getSlaves();
        //The list of slaves that have the packet in question
        List<Slave> havePacket = new ArrayList<>();

        if(checkRecentlyRebroadcasted(packetID)){
            return true;
        }

        synchronized (slaves){
            for(Slave slave : slaves) {
                if(slave.hasPacket(packetID)) {
                    havePacket.add(slave);

                }
            }



            //If no slaves have the packet return false, but if they all have it return true.
            if(havePacket.size() == 0){
                return false;
            } else if( havePacket.size() == slaves.size()){
                return true;
            }
        }



        int index = mRandom.nextInt(havePacket.size());

        Slave slave = havePacket.get(index);
        Log.d(LOG_TAG , "Telling "  + havePacket.get(index).toString() + " to rebroadcast frame #" + packetID);

        synchronized (mOutputStreams) {
            OutputStream stream = mOutputStreams.get(slave);
            TCPRetransmitPacket.send(stream, packetID);
        }

        mRecentlyRebroadcasted.put(packetID , System.currentTimeMillis());
        return true;
    }

    private boolean checkRecentlyRebroadcasted(int packetID){
        synchronized (mRecentlyRebroadcasted) {
            ArrayList<Integer> toRemove = new ArrayList<>();
            for (Map.Entry<Integer, Long> entry : mRecentlyRebroadcasted.entrySet()) {
                if (System.currentTimeMillis() - entry.getValue() >= 25) {
                    toRemove.add(entry.getKey());
                }
            }
            for(Integer i : toRemove){
                mRecentlyRebroadcasted.remove(i);
            }
        }
        if (mRecentlyRebroadcasted.containsKey(packetID)) {
            return true;
        }
        return false;
    }

    public void startNewConnection(DatagramPacket packet){
        InetAddress addr = packet.getAddress();
        Socket socket = null;
        try {
             socket = new Socket(addr, CONSTANTS.RELIABILITY_PORT);
        } catch(IOException e){
            e.printStackTrace();
        }



    }

    private long mSongStart;
    private int mChannels;
    private byte mStreamID;
    public void startSong(long songStart, int channels ,byte streamID ){


        mSongStart = songStart;
        mChannels = channels;
        //TODO: see about if deleting this is neccessary
        mChannels = 2;
        mStreamID = streamID;

        getStartThread().start();

    }

    private Thread getStartThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                notifyOfSongStart();
            }
        });
    }

    private void notifyOfSongStart(){
        Log.d(LOG_TAG , "Notifying all listeners of song start");
        long begin = System.currentTimeMillis();
        synchronized (mOutputStreams) {
            for (Map.Entry<Slave, DataOutputStream> entry : mOutputStreams.entrySet()) {

                TCPSongStartPacket.send(entry.getValue(), mBroadcaster.getSongStartTime(),
                        mBroadcaster.getChannels(), mBroadcaster.getStreamID());
            }
        }
        Log.d(LOG_TAG , "Done notifying after :" + (System.currentTimeMillis() - begin) + "ms.");
    }

    private void sendSongInProgress(Slave slave) {
        Log.d(LOG_TAG, "Sending Song Start to "  + slave.toString());
        DataOutputStream outputStream;
        synchronized (mOutputStreams) {

            //Send out the Song In Progress TCP packet.
            TCPSongInProgressPacket.send(mOutputStreams.get(slave), mBroadcaster.getSongStartTime(), mBroadcaster.getChannels(),
                    mBroadcaster.getNextPacketSendID(), mBroadcaster.getStreamID());
        }


    }

    //Send a TCP packet to the one that needs it containing an AAC frame
    public void sendFrameTCP(AudioFrame frame , Slave slave){
        DataOutputStream stream;
        synchronized (mOutputStreams) {
             stream = mOutputStreams.get(slave);
             TCPFramePacket.send(stream, frame, mBroadcaster.getStreamID());
        }


    }

    public synchronized void pause(){
        Log.d(LOG_TAG , "Pausing");
        synchronized (mOutputStreams) {
            for (Map.Entry<Slave, DataOutputStream> entry : mOutputStreams.entrySet()) {
                DataOutputStream stream = entry.getValue();
                TCPPausePacket.send(stream);
            }
        }

    }

    //Sends the resume command to all Slaves
    public void resume(long resumeTime, long newSongStartTime){

        synchronized (mOutputStreams){
            for (Map.Entry<Slave, DataOutputStream> entry : mOutputStreams.entrySet()) {

                DataOutputStream stream = entry.getValue();
                TCPResumePacket.send(stream, resumeTime, newSongStartTime);

            }
        }

    }

    public void destroy(){
        mRunning = false;
        for (Map.Entry<Slave, Socket> entry : mSockets.entrySet()){

            Socket socket = entry.getValue();
            synchronized (socket) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try{
            mServerSocket.close();
        } catch (IOException e){
            e.printStackTrace();
        }


    }

    public void seek(long seekTime){
        synchronized (mOutputStreams) {
            for (Map.Entry<Slave, DataOutputStream> entry : mOutputStreams.entrySet()) {

                DataOutputStream stream = entry.getValue();
                TCPSeekPacket.send(stream, seekTime);
            }
        }
    }

    private Thread getSendPacketThread(int packet , Slave slave){
        return new Thread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    //Sends all listeners a command indicating the ID of the last packet.
    public void lastPacket(int ID){
        synchronized (mOutputStreams) {
            for (Map.Entry<Slave, DataOutputStream> entry : mOutputStreams.entrySet()) {
                DataOutputStream stream = entry.getValue();
                TCPLastFramePacket.send(stream, ID);
            }
        }

    }


    //Instructs a host to retransmit the 0 packet.
    public void retransmit(){
        synchronized (mOutputStreams){
            for (Map.Entry<Slave, DataOutputStream> entry : mOutputStreams.entrySet()){

                DataOutputStream stream = entry.getValue();
                TCPRetransmitPacket.send(stream, 0);
            }
        }

    }
}
