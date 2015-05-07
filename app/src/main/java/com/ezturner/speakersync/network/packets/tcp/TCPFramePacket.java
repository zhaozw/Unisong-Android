package com.ezturner.speakersync.network.packets.tcp;

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

    private AudioFrame mFrame;

    public TCPFramePacket(InputStream stream)throws IOException{
        receive(stream);
    }

    public static void send(OutputStream stream , long playTime , byte streamID , int frameID, byte[] aacData) throws  IOException{

        //Throw the frame Id in a byte array, and throw all the other data in byte
        // arrays and combine them into the data that will go in the packet
        byte[] frameIDArr = ByteBuffer.allocate(4).putInt(frameID).array();

        byte[] playTimeArr = ByteBuffer.allocate(8).putLong(playTime).array();

        byte[] dataSizeArr = ByteBuffer.allocate(4).putInt(aacData.length).array();

        byte[] data = NetworkUtilities.combineArrays(frameIDArr, playTimeArr);

        data = NetworkUtilities.combineArrays(data , dataSizeArr);

        byte[] streamIDArr = NetworkUtilities.combineArrays(data ,new byte[]{streamID} );

        stream.write(data);

        stream.write(aacData);
    }

    private void receive(InputStream stream) throws IOException{
        byte[] data = new byte[17];

        stream.read(data);

        byte[] frameIDArr = Arrays.copyOfRange(data, 0, 4);

        int ID = ByteBuffer.wrap(frameIDArr).getInt();

        byte[] playTimeArr = Arrays.copyOfRange(data, 4, 12);

        long playTime = ByteBuffer.wrap(playTimeArr).getLong();

        byte[] dataSizeArr = Arrays.copyOfRange(data, 12, 16);

        int dataSize = ByteBuffer.wrap(dataSizeArr).getInt();

        byte streamID = data[16];

        byte[] aacData = new byte[dataSize];

        stream.read(aacData);

        mFrame = new AudioFrame(aacData , ID , playTime);
    }

    public AudioFrame getFrame(){
        return mFrame;
    }

}
