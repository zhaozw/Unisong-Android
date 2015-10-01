package io.unisong.android.network.host;

import android.util.Log;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.NetworkUtilities;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class encodes the Audio Frames into
 * Created by Ethan on 5/15/2015.
 */
public class MasterFECHandler {

    private final String LOG_TAG = MasterTCPHandler.class.getSimpleName();

    private Broadcaster mBroadcaster;

    private Map<Integer, AudioFrame> mFrames;
    private int mTopFrame = 0;

    //The Index for mFrames
    private int mCurrentFrame = 0;

    //The index for data within the frames
    private int mDataIndex = 0;

    private int mDataAvailable = 0;

    private Thread mEncodeThread;

    private boolean mRunning;

    public MasterFECHandler(){
        mFrames = new TreeMap<>();
        mEncodeThread = getThread();
        mEncodeThread.start();
        mRunning = true;
    }

    public void addFrames(ArrayList<AudioFrame> frames){
        synchronized (mFrames){
            synchronized (mFrames) {
                for (AudioFrame frame : frames) {
                    if(frame.getID() > mTopFrame)   mTopFrame = frame.getID();
                    mFrames.put(frame.getID(), frame);
                    mDataAvailable += frame.getData().length + 8;
                }
            }
        }

        synchronized (mEncodeThread){
            mEncodeThread.notify();
        }
    }



    private Thread getThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                encode();
            }
        });
    }


    private void encode(){
        while(mRunning){
            Log.d(LOG_TAG , "Checking, data available is : " + mDataAvailable + " and data length is : " + CONSTANTS.FEC_DATA_LENGTH);
            if(mDataAvailable >= CONSTANTS.FEC_DATA_LENGTH){
                Log.d(LOG_TAG , "Creating Data");
                byte[] srcData = createFrameArray();


//                ArrayDataEncoder encoder = OpenRQ.newEncoder(srcData, );



                /*

                // send all source symbols
                for (EncodingPacket pac : sbEnc.sourcePacketsIterable()) {
                    sendPacket(pac);
                }

                // number of repair symbols
                int nr = numberOfRepairSymbols();

                // send nr repair symbols
                for (EncodingPacket pac : sbEnc.repairPacketsIterable(nr)) {
                    sendPacket(pac);
                }*/


            } else {
                synchronized (mEncodeThread){
                    try {
                        mEncodeThread.wait();
                    } catch (InterruptedException e){

                    }
                }
            }
        }
    }

    /*
    private static void sendPacket(EncodingPacket pac) {

        // send the packet to the receiver
    }*/


    //TODO: have this work with Dynamic Network Adaptation Technology to adapt to the network as we go
    private int numberOfRepairSymbols(){
        return 4;
    }

    private byte[] createFrameArray(){

        //The destination byte array for loading frames and their sizes
        byte[] dstData = new byte[CONSTANTS.FEC_DATA_LENGTH];
        int dstDataIndex = 0;

        synchronized (mFrames) {
            for (int i = 0; mCurrentFrame < mTopFrame && dstDataIndex != dstData.length - 1; mCurrentFrame++) {

                AudioFrame frame = mFrames.get(mCurrentFrame);

                byte[] frameData = createFrameData(frame);
                mDataAvailable -= setData(frameData, dstDataIndex, dstData);
            }
        }


        return dstData;
    }

    //TODO: test this 
    private int setData(byte[] srcData , int dstDataIndex, byte[] dstData){

        int spaceLeft = CONSTANTS.FEC_DATA_LENGTH - dstDataIndex;

        int startIndex = 0;
        int endIndex = 0;

        //If we've got enough space for the whole rest of the frame, then use the rest of the frame
        if(spaceLeft > (srcData.length - mDataIndex)){

            startIndex = mDataIndex;
            if(mDataIndex != 0){
                endIndex = srcData.length;
                mDataIndex = 0;
            }
            mCurrentFrame++;

        } else {
            //If there isn't enough space left in the destination data, then let's copy what we can.
            startIndex = mDataIndex;

            if(spaceLeft >= srcData.length - mDataIndex){
                mDataIndex += spaceLeft;
                endIndex = startIndex += spaceLeft;
            }

        }

        int numBytes = 0;
        //Lets assign the data to the destination array
        while(startIndex < endIndex){
            numBytes++;
            dstData[dstDataIndex] = srcData[startIndex];
            startIndex++;
            dstDataIndex++;
        }

        return numBytes;
    }

    private byte[] createFrameData(AudioFrame frame){
        //The header
        byte[] IDArr = ByteBuffer.allocate(4).putInt(frame.getID()).array();
        byte[] sizeArr = ByteBuffer.allocate(4).putInt(frame.getData().length).array();

        //Source AAC data
        byte[] srcData = frame.getData();


        //Combine the header
        byte[] header = NetworkUtilities.combineArrays(IDArr , sizeArr);
        byte[] data = NetworkUtilities.combineArrays(header , srcData);

        return data;
    }

    public void destroy(){
        mRunning = false;
    }

}
