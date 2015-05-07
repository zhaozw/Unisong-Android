package com.ezturner.speakersync.network.master;

import android.util.Log;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.packets.tcp.TCPSongInProgressPacket;
import com.ezturner.speakersync.network.packets.tcp.TCPSongStartPacket;

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

    private String LOG_TAG = "MasterReliabilityHandler";

    //The listener for when a client joins the session and starts a TCP handshake
    private ServerSocket mServerSocket;

    //The list of all of the TCP control sockets that are listening for reliability packets
    private Map<Slave ,Socket> mSockets;

    //The AudioBroadcaster that this class interfaces with
    private AudioBroadcaster mBroadcaster;

    //The thread that'll listen to reliability packets
    private Thread mServerSocketThread;

    private List<Thread> mSocketThreads;

    private boolean mRunning;

    //The random number generator for choosing which slave to have as a rebroadcaster
    private Random mRandom;
    //TODO: Implement passive listening using AudioBroadcaster.DISOVERY_PASSIVE_PORT

    public MasterTCPHandler(AudioBroadcaster broadcaster){

        mBroadcaster = broadcaster;

        mRandom = new Random();

        try {
            
            mServerSocket = new ServerSocket(CONSTANTS.RELIABILITY_PORT);

        } catch(IOException e){
            e.printStackTrace();
        }

        mSockets = new HashMap<Slave, Socket>();
        mSocketThreads = new ArrayList<>();

        mServerSocketThread = startReliabilityConnectionListener();

        mRunning = true;
        mServerSocketThread.start();
    }

    //Starts the listener for new connections
    private Thread startReliabilityConnectionListener(){
        return new Thread(){
            public void run(){
                while(mRunning){
                    Socket socket = null;

                    Log.d(LOG_TAG , "Starting to listen for sockets");
                    try {
                        socket = mServerSocket.accept();
                    } catch(IOException e){
                        e.printStackTrace();
                    }
                    Log.d(LOG_TAG , "Socket connected : " + socket.getInetAddress());

                    if(socket != null){

                        if(mBroadcaster.isStreamRunning()) {
                            sendSongInProgress(socket);
                        }
                        Thread thread = startSocketListener(socket);
                        mSocketThreads.add(thread);
                        thread.start();
                    }
                }
            }
        };
    }

    //Start listening for packets
    private Thread startSocketListener(Socket socket){
        class SocketRunnable implements Runnable {
            Socket socket;
            SocketRunnable(Socket s) { socket = s; }
            public void run(){
                try {
                    listenToReliabilitySocket(socket);
                } catch(IOException e){
                    e.printStackTrace();
                }
            }
        };

        return new Thread(new SocketRunnable(socket));
    }

    //Handle new data coming in from a Reliability socket
    private void listenToReliabilitySocket(Socket socket) throws IOException{
        //TODO: see if we need to get rid of the port for this to work
        Slave slave = new Slave(socket.getRemoteSocketAddress().toString());
        mBroadcaster.addSlave(slave);
        mSockets.put(slave, socket);
        InputStream is = socket.getInputStream();

        byte[] inputArr = new byte[5];

        int identifier = -2;
        while((identifier = is.read()) != -1){
            handleDataReceived(identifier, slave , is);
        }

        if(socket.isClosed()){
            mBroadcaster.removeSlave(slave);
        }
    }


    //Hanldes the identifying byte and redirects it to the right method
    private void handleDataReceived(int identifier , Slave slave , InputStream inputStream){
        switch (identifier){
            case CONSTANTS.TCP_REQUEST:
                int packetID = getInt(inputStream);
                if(packetID != -1 || !checkSlaves(packetID))  mBroadcaster.rebroadcastPacket(packetID);
                break;
            case CONSTANTS.TCP_ACK:
                int ID = getInt(inputStream);
                if(ID != -1)    slave.packetReceived(ID);
                break;
        }
    }

    private int getInt(InputStream inputStream){
        byte[] data = new byte[4];

        try {
            synchronized (inputStream) {
                inputStream.read(data);
            }
        } catch (IOException e){
            e.printStackTrace();
            return -1;
        }

        return ByteBuffer.wrap(data).getInt();
    }

    //Checks to see if any of the slaves have the packet in question.
    public boolean checkSlaves(int packetID){
        List<Slave> slaves = mBroadcaster.getSlaves();
        //The list of slaves that have the packet in question
        List<Slave> havePacket = new ArrayList<>();

        synchronized (slaves){
            for(Slave slave : slaves) {
                if(slave.hasPacket(packetID)) {
                    havePacket.add(slave);
                }
            }
        }
        if(havePacket.size() == 0){
            return false;
        }


        int index = mRandom.nextInt(havePacket.size());

        Socket socket = mSockets.get(havePacket.get(index));
        byte[] idArr = ByteBuffer.allocate(4).putInt(packetID).array();

        try {
            OutputStream stream = socket.getOutputStream();
            synchronized (stream) {
                Log.d(LOG_TAG, "Instructing " + havePacket.get(index) + " ");
                stream.write(CONSTANTS.TCP_COMMAND_RETRANSMIT);
                stream.write(idArr);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return true;
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
        for (Map.Entry<Slave, Socket> entry : mSockets.entrySet()){
            Log.d(LOG_TAG , "Starting song for slave : " + entry.getKey() );
            Socket socket = entry.getValue();
            synchronized (socket){
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    TCPSongStartPacket.send(outputStream, mBroadcaster.getSongStartTime() ,
                            mBroadcaster.getChannels() , mBroadcaster.getStreamID());

                } catch (IOException e){
                    e.printStackTrace();
                }
            }

        }
    }

    private void sendSongInProgress(Socket socket){
        try{
            OutputStream stream = socket.getOutputStream();

            //Send out the Song In Progress TCP packet.
            TCPSongInProgressPacket.send(stream, mBroadcaster.getSongStartTime() , mBroadcaster.getChannels(),
                    mBroadcaster.getNextPacketSendID() , mBroadcaster.getStreamID());

        } catch (IOException e){
            e.printStackTrace();
        }
    }

}
