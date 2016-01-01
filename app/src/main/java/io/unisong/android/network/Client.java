package io.unisong.android.network;

import android.util.Log;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.network.host.MasterTCPHandler;
import io.unisong.android.network.host.transmitter.LANTransmitter;
import io.unisong.android.network.packets.tcp.TCPAcknowledgePacket;
import io.unisong.android.network.packets.tcp.TCPEndSongPacket;
import io.unisong.android.network.packets.tcp.TCPFramePacket;
import io.unisong.android.network.packets.tcp.TCPPausePacket;
import io.unisong.android.network.packets.tcp.TCPRequestPacket;
import io.unisong.android.network.packets.tcp.TCPResumePacket;
import io.unisong.android.network.packets.tcp.TCPSeekPacket;
import io.unisong.android.network.packets.tcp.TCPSongInProgressPacket;
import io.unisong.android.network.packets.tcp.TCPSongStartPacket;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ezturner on 4/29/2015.
 */
public class Client {

    private final byte ADDRESS = 0;
    private final byte USER = 1;
    private static final String LOG_TAG = Client.class.getSimpleName();
    private InetAddress mAddress;

    //A list of all of the packets that this slave has received and has in memory.
    private List<Integer> mPacketsReceived;
    private Map<Integer, Long> mPacketsRebroadcasted;
    private boolean mConnected;
    private Socket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    private boolean mThreadRunning;
    private Thread mListenThread;
    private MasterTCPHandler mMasterTCPHandler;

    //The Singletons
    private AudioStatePublisher mAudioStatePublisher;
    private TimeManager mTimeManager;

    private LANTransmitter mTransmitter;
    private User mUser;

    // The Unisong Session we're currently in
    private UnisongSession mSession;


    //TODO: add a way to connect a user

    /**
     * This is a constructor for a locally connected client, from the master to client
     * this constructor will be called by the master
     * @param ip The device IP
     * @param socket the socket that it is connected to
     * @param parent the TCP handler that holds us as its parent
     * @param transmitter the LANTransmitter that we are using for this session
     */
    //TODO : Add mode for server connection
    public Client(String ip, Socket socket, MasterTCPHandler parent, LANTransmitter transmitter){

        mSession = CurrentUser.getInstance().getSession();
        mTransmitter = transmitter;

        //Get the singleton objects.
        mTimeManager = TimeManager.getInstance();
        mAudioStatePublisher = AudioStatePublisher.getInstance();

        mMasterTCPHandler = parent;
        mSocket = socket;

        try{
            mInputStream = new BufferedInputStream(mSocket.getInputStream());
            mOutputStream = new BufferedOutputStream(mSocket.getOutputStream());
        } catch (IOException e){
            e.printStackTrace();
            mConnected = false;
        }

        mConnected = true;

        try {
            mAddress = Inet4Address.getByName(ip.split(":")[0].substring(1));
        } catch (UnknownHostException e){
            e.printStackTrace();
            Log.e(LOG_TAG, "Unknown host address when creating slave : " + ip);
        }

        mPacketsRebroadcasted = new HashMap<>();
        mPacketsReceived = new ArrayList<>();

        mListenThread = getListenThread();
        mListenThread.start();
    }


    /**
     * The constructor for client information for a non-master
     * This constructor is used when a client is discovered through the UDP discovery process
     *
     */
    public Client(String ip, User user){
        mConnected = false;


    }

    /**
     * The constructor used to obtain information about a client from a TCP
     * connection.
     * @param stream the network input stream that we will read this client from
     */
    public Client(InputStream stream){
        byte[] sizeArr = new byte[4];

        try {
            NetworkUtilities.readFromStream(stream, sizeArr);
        } catch (IOException e){
            e.printStackTrace();
        }

        int size = ByteBuffer.wrap(sizeArr).getInt();

        byte[] data = new byte[size];

        try{
            NetworkUtilities.readFromStream(stream , data);
        } catch (IOException e){
            e.printStackTrace();
        }

        decode(data , 0);
    }

    /**
     * This is a recursive method to decode a byte array into a Client object
     *
     * @param data the raw network data for this client object
     * @param index the index at which we are currently operating
     */
    //TODO: test and make sure this recursion works.
    private void decode(byte[] data , int index){
        //Increment the index for the switch statement
        index++;
        switch (data[index - 1]){
            case USER:
                decodeUser(data , index);
                break;
            case ADDRESS:
                decodeAddress(data , index);
                break;
        }

        // If the index is either greater than the data length or at the end of the data, then
        // return and don't recursively call
        if(index< data.length -1 ){
            decode(data , index);
        }
    }

    // TODO : delete
    private void decodeUser(byte[] data , int index){
        byte[] sizeArr = Arrays.copyOfRange(data , index , index + 4);
        index += 4;

        int size = ByteBuffer.wrap(sizeArr).getInt();

        byte[] usrDataArr = Arrays.copyOfRange(data , index, index + size);
        index += size;

    }

    private void decodeAddress(byte[] data, int index){
        byte[] sizeArr = Arrays.copyOfRange(data , index , index + 4);
        index += 4;

        int size = ByteBuffer.wrap(sizeArr).getInt();

        byte[] addrArr = Arrays.copyOfRange(data, index, index + size);
        index += size;

        //If the byte array is 4 byes long then
        if(addrArr.length == 4){
            try {
                mAddress = Inet4Address.getByAddress(addrArr);
            } catch (UnknownHostException e){
                e.printStackTrace();
            }
        } else {
            try {
                mAddress = Inet6Address.getByAddress(addrArr);
            } catch (UnknownHostException e){
                e.printStackTrace();
            }
        }



    }

    public void packetReceived(int ID){
        mPacketsReceived.add(ID);
        if(mPacketsRebroadcasted.containsKey(ID)){
            mPacketsRebroadcasted.remove(ID);
        }
    }

    public boolean hasPacket(int ID){
        return mPacketsReceived.contains(ID);
    }

    public void packetHasBeenRebroadcasted(int ID){
        if(!mPacketsReceived.contains(ID)) {
            mPacketsRebroadcasted.put(ID, System.currentTimeMillis());
        }
    }

    public String toString(){
        return "Client, IP: " + mAddress.toString();
    }

    public List<Integer> getPacketsToBeReSent(){
        List<Integer> ids = new ArrayList<>();

        ArrayList<Integer> packetsSent = new ArrayList<>();
        synchronized (mPacketsRebroadcasted){
            for (Map.Entry<Integer, Long> entry : mPacketsRebroadcasted.entrySet()) {
                if (System.currentTimeMillis() - entry.getValue() >= 150) {
                    ids.add(entry.getKey());
                    packetsSent.add(entry.getKey());
                }
            }
        }

        for(Integer i : packetsSent){
            mPacketsRebroadcasted.remove(i);
            mPacketsRebroadcasted.put(i, System.currentTimeMillis());
        }

        return ids;
    }

    //Start listening for packets
    private Thread getListenThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                mThreadRunning = true;
                listenForPackets();
                mThreadRunning = false;
            }
        });
    }

    private void listenForPackets(){
        //Handle new data coming in from a Reliability socket
        //TODO: see if we need to get rid of the port for this to work

        //If we are playing, then just tell the client that we are playing a song in progress
        if(mAudioStatePublisher.getState() == AudioStatePublisher.PLAYING) {

            sendSongInProgress();

        } else if(mAudioStatePublisher.getState() == AudioStatePublisher.PAUSED){

            //If we are paused, tell the client we are playing a song in progress and then pause
            sendSongInProgress();
            pause();

        } else if(mAudioStatePublisher.getState() == AudioStatePublisher.RESUME){

            //If we are in the resume stage, then wait 10ms and notify of a song in progress
            try {
                synchronized (this) {
                    this.wait(10);
                }
            } catch (InterruptedException e){

            }
            sendSongInProgress();
        }

        byte type;
        synchronized (mInputStream) {
            try {
                type = (byte) mInputStream.read();
            } catch (IOException e){
                e.printStackTrace();
                mConnected = false;
                return;
            }
        }

        while(type != -1 && mConnected){
            handleDataReceived(type);
            try {
                type = (byte) mInputStream.read();
            } catch (IOException e){
                e.printStackTrace();
                mConnected = false;
                return;
            }
        }

    }

    //Hanldes the identifying byte and redirects it to the right method
    private void handleDataReceived(byte type){
        switch (type){
            case CONSTANTS.TCP_REQUEST:
                int packetID = new TCPRequestPacket(mInputStream).getPacketRequested();
                if(packetID != -1)  mMasterTCPHandler.rebroadcastFrame(packetID);
                break;
            case CONSTANTS.TCP_ACK:
                int ID = new TCPAcknowledgePacket(mInputStream).getPacketAcknowledged();
                if(ID != -1)    packetReceived(ID);
                break;
        }
    }

    //Tells this slave that a song is currently in progress
    public void sendSongInProgress() {
        Log.d(LOG_TAG, "Sending UISong Start to "  + toString());
        DataOutputStream outputStream;
        synchronized (mOutputStream){
            //Send out the UISong In Progress TCP packet.
            //TCPSongInProgressPacket.send(mOutputStream, mTimeManager.getSongStartTime(), mSession.getCurrentSong().getChannels(),
            //        mTransmitter.getNextPacketSendID(), (byte) 0);
        }
    }

    //Notifies this slave that a song is starting
    public void notifyOfSongStart(){
        synchronized (mOutputStream){
            //TCPSongStartPacket.send(mOutputStream , mTimeManager.getSongStartTime() , mSession.getCurrentSong().getChannels() , (byte) 0 );
        }
    }

    public void retransmitPacket(int packetID){
        synchronized (mOutputStream){
            TCPRequestPacket.send(mOutputStream, packetID);
        }
    }

    public void sendFrame(AudioFrame frame){
        synchronized (mOutputStream){
            TCPFramePacket.send(mOutputStream , frame, (byte) 0);
        }
    }

    public boolean isConnected(){
        return mConnected;
    }

    public void pause(){
        synchronized (mOutputStream){
            TCPPausePacket.send(mOutputStream);
        }
    }

    public void resume(long resumeTime, long newSongStartTime){
        synchronized (mOutputStream){
            TCPResumePacket.send(mOutputStream , resumeTime , newSongStartTime );
        }
    }

    public void seek(long seekTime){
        synchronized (mOutputStream){
            TCPSeekPacket.send(mOutputStream, seekTime);
        }
    }

    public void endSong(byte streamID){
        synchronized (mOutputStream){
            TCPEndSongPacket.send(mOutputStream, streamID);
        }
    }

    public void destroy(){
        mConnected = false;
        while(mThreadRunning){}
        mTransmitter = null;
        mMasterTCPHandler = null;
    }

    public InetAddress getAddress(){
        return mAddress;
    }

    //Checks if there is a user associated with this client
    public boolean hasUser(){
        if(mUser == null){
            return false;
        } else {
            return true;
        }
    }

    /**
     * Parses all of this client's information and any relevant flags into a
     * byte array, to be decoded and reconstructed on the target device.
     *
     * The format is as follows:
     * DATA_ID
     * DATA_LENGTH (if variable)
     * DATA
     *
     * And then repeat
     * @return returns this client object represented by a byte array
     */
    public byte[] getBytes(){
        byte[] addressHeaderArr = new byte[]{ADDRESS};
        byte[] addressLengthArr;
        byte[] addressArr = getAddress().getAddress();

        addressLengthArr = ByteBuffer.allocate(4).putInt(addressArr.length).array();

        byte[] data = NetworkUtilities.combineArrays(addressHeaderArr , addressLengthArr);

        data = NetworkUtilities.combineArrays(data , addressArr);


        return data;
    }

    /**
     * Check whether this client is equal to another
     * This method is used to ensure that we do not get duplicates through
     * @param client
     */
    public boolean equals(Client client){
        //TODO: improve this as this class expands.
        //Check to see if the addresses are different
        if(getAddress() != null && client.getAddress() != null &&
            !getAddress().equals(client.getAddress())){
            return false;
        }
        return true;
    }


}