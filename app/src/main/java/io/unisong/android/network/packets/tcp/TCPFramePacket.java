package io.unisong.android.network.packets.tcp;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.NetworkUtilities;

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

        Log.d(LOG_TAG , "Frame #" + frame.getID() + " has " + frame.getData().length + " bytes.");
        synchronized (stream){
            try {
                stream.write(CONSTANTS.TCP_FRAME);
                stream.write(data);
                stream.write(frame.getData());
                stream.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void receive(InputStream stream){
        byte[] data = new byte[9];

        synchronized (stream) {
            try {
                NetworkUtilities.readFromStream(stream, data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        byte[] frameIDArr = Arrays.copyOfRange(data, 0, 4);

        int ID = ByteBuffer.wrap(frameIDArr).getInt();

        byte[] dataSizeArr = Arrays.copyOfRange(data, 4, 8);

        int dataSize = ByteBuffer.wrap(dataSizeArr).getInt();
        Log.d(LOG_TAG , "Data size is : " + dataSize + " for frame #" + ID);
        byte streamID = data[8];

        byte[] aacData = new byte[dataSize];

        synchronized (stream) {
            try {
                NetworkUtilities.readFromStream(stream, aacData);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        mFrame = new AudioFrame(aacData , ID , streamID);
    }

    public AudioFrame getFrame(){
        return mFrame;
    }

}
