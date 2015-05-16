package com.ezturner.speakersync.network.packets.tcp;

import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by ezturner on 5/6/2015.
 */
public class TCPFramePacket {

    private final static String LOG_TAG = TCPFramePacket.class.getSimpleName();
    private AudioFrame mFrame;

    public TCPFramePacket(InputStream stream){
        receive(stream);
    }

    public static void send(OutputStream stream ,AudioFrame frame ,  byte streamID){

        //Throw the frame Id in a byte array, and throw all the other data in byte
        // arrays and combine them into the data that will go in the packet
        byte[] frameIDArr = ByteBuffer.allocate(4).putInt(frame.getID()).array();

        byte[] dataSizeArr = ByteBuffer.allocate(4).putInt(frame.getData().length).array();

        byte[] data = NetworkUtilities.combineArrays(frameIDArr , dataSizeArr);

        byte[] streamIDArr = new byte[]{streamID};

        data = NetworkUtilities.combineArrays(data ,streamIDArr);

        synchronized (stream){
            try {
                stream.write(CONSTANTS.TCP_FRAME);
                stream.write(data);
                stream.write(frame.getData());
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void receive(InputStream stream){
        byte[] data = new byte[9];

        synchronized (stream) {
            try {
                stream.read(data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        byte[] frameIDArr = Arrays.copyOfRange(data, 0, 4);

        int ID = ByteBuffer.wrap(frameIDArr).getInt();

        byte[] dataSizeArr = Arrays.copyOfRange(data, 4, 8);

        int dataSize = ByteBuffer.wrap(dataSizeArr).getInt();

        byte streamID = data[8];

        byte[] aacData = new byte[dataSize];

        synchronized (stream) {
            try {
                stream.read(aacData);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        mFrame = new AudioFrame(aacData , ID );
    }

    public AudioFrame getFrame(){
        return mFrame;
    }

}
