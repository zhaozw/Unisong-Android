package com.ezturner.speakersync.network.master;

import android.util.Log;

import com.ezturner.speakersync.network.CONSTANTS;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
public class MasterReliabilityHandler {

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

    public MasterReliabilityHandler(AudioBroadcaster broadcaster){

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
                        startSocketListener(socket).start();

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

        while((is.read(inputArr , 0, 5)) != -1){
            handleDataReceived(inputArr, slave);
        }

        if(socket.isClosed()){
            mBroadcaster.removeSlave(slave);
        }
    }

    private void handleDataReceived(byte[] data , Slave slave){
        switch (data[0]){
            case CONSTANTS.TCP_REQUEST_ID:
                int packetID = ByteBuffer.wrap(new byte[]{data[1] , data[2] , data[3] , data[4]}).getInt();
                if(!checkSlaves(packetID))  mBroadcaster.rebroadcastPacket(packetID);
                break;
            case CONSTANTS.TCP_ACK_ID:
                int ID = ByteBuffer.wrap(new byte[]{data[1] , data[2] , data[3] , data[4]}).getInt();
                slave.packetReceived(ID);
                break;
        }
    }

    //Checks to see if any of the slaves have the packet in question.
    private boolean checkSlaves(int packetID){
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

        byte[] data = new byte[] {CONSTANTS.TCP_COMMAND_RETRANSMIT , idArr[0] , idArr[1] , idArr[2] , idArr[3]};
        try {
            socket.getOutputStream().write(data);
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


        /* Not used atm, UDP code for reliability
    private void handleReliabilityPacket(DatagramPacket packet){
        //Get data
        byte[] data = packet.getData();

        //Feed the data into a Bytebuffer
        ByteBuffer wrapped = ByteBuffer.wrap(data);

        //get stream ID and check it against current
        byte requestedStreamID = wrapped.get();

        if(requestedStreamID != streamID){
            return;
        }

        //Convert data to an int
        int packetID = wrapped.getInt();

        broadcastStreamPacket(packetID);
    }*/
}
