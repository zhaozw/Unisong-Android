package com.ezturner.speakersync.network.master;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by Ethan on 2/11/2015.
 */
public class MasterReliabilityHandler {

    private String LOG_TAG = "MasterReliabilityHandler";

    public static final int PACKET_SENT_CODE = 55;

    //The listener for when a client joins the session and starts a TCP handshake
    private ServerSocket mServerSocket;

    //The list of all of the TCP control sockets that are listening for reliability packets
    private ArrayList<Socket> mSockets;

    //The AudioBroadcaster that this class interfaces with
    private AudioBroadcaster mBroadcaster;

    //The thread that'll listen to reliability packets
    private Thread mServerSocketThread;

    //TODO: Implement passive listening using AudioBroadcaster.DISOVERY_PASSIVE_PORT

    public MasterReliabilityHandler(AudioBroadcaster broadcaster){

        mBroadcaster = broadcaster;

        try {
            
            mServerSocket = new ServerSocket(broadcaster.getPort());

        } catch(IOException e){
            e.printStackTrace();
        }

        mSockets = new ArrayList<Socket>();

        mServerSocketThread = startReliabilityConnectionListener();

        mServerSocketThread.start();
    }

    //Starts the listener for new connections
    private Thread startReliabilityConnectionListener(){
        return new Thread(){
            public void run(){
                while(mBroadcaster.isStreamRunning()){
                    Socket socket = null;

                    try {
                        socket = mServerSocket.accept();
                    } catch(IOException e){
                        e.printStackTrace();
                    }

                    if(socket != null){
                        mSockets.add(socket);
                        startSocketListener(socket).start();
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
            public void run() {
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
        mSockets.add(socket);
        BufferedReader in =
                new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        String inputLine;
        long startTime = System.currentTimeMillis();
        while((inputLine = in.readLine()) != null) {
            String data = in.readLine();
            Log.d(LOG_TAG, "Data received: " + data);
            int packetID = Integer.parseInt(inputLine);

            mBroadcaster.rebroadcastPacket(packetID);

        }
    }

    public void startNewConnection(DatagramPacket packet){
        InetAddress addr = packet.getAddress();
        Socket socket = null;
        try {
             socket = new Socket(addr, mBroadcaster.getPort());
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
