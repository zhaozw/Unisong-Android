package com.ezturner.speakersync.network.master;

import android.util.Log;

import com.ezturner.speakersync.audio.AudioFrame;
import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import net.fec.openrq.ArrayDataEncoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.encoder.SourceBlockEncoder;
import net.fec.openrq.parameters.FECParameters;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class encodes the Audio Frames into
 * Created by Ethan on 5/15/2015.
 */
public class MasterFECHandler {

    private final String LOG_TAG = MasterTCPHandler.class.getSimpleName();

    private Broadcaster mBroadcaster;

    private ArrayList<AudioFrame> mFrames;
    private ArrayList<byte[]> mFrameDatas;

    //The Index for mFrames
    private int mCurrentFrame = 0;

    //The index for data within the frames
    private int mDataIndex = 0;

    private int mDataAvailable = 0;

    private Thread mEncodeThread;

    private boolean mRunning;

    public MasterFECHandler(Broadcaster broadcaster){
        mBroadcaster =broadcaster;
        mFrames = new ArrayList<>();
        mFrameDatas = new ArrayList<>();
        mEncodeThread = getThread();
        mEncodeThread.start();
        mRunning = true;
    }

    public void addFrames(ArrayList<AudioFrame> frames){
        synchronized (mFrames){
            synchronized (mFrameDatas) {
                for (AudioFrame frame : frames) {
                    mFrames.add(frame);
                    byte[] data = createFrameData(frame);
                    mFrameDatas.add(data);
                    mDataAvailable += data.length;
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


                FECParameters parameters = FECParameters.newParameters(CONSTANTS.FEC_DATA_LENGTH,
                        CONSTANTS.FEC_SYMBOL_SIZE , CONSTANTS.FEC_DATA_LENGTH / CONSTANTS.FEC_SYMBOL_SIZE);
                ArrayDataEncoder encoder = OpenRQ.newEncoder(srcData, );




                // send all source symbols
                for (EncodingPacket pac : sbEnc.sourcePacketsIterable()) {
                    sendPacket(pac);
                }

                // number of repair symbols
                int nr = numberOfRepairSymbols();

                // send nr repair symbols
                for (EncodingPacket pac : sbEnc.repairPacketsIterable(nr)) {
                    sendPacket(pac);
                }


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
            for (int i = 0; mCurrentFrame < mFrameDatas.size() && dstDataIndex != dstData.length - 1; mCurrentFrame++) {

                byte[] frameData = mFrameDatas.get(mCurrentFrame);

                mDataAvailable -= setData(frameData, dstDataIndex, dstData);
            }
        }


        return dstData;
    }

    private int setData(byte[] srcData , int dstDataIndex, byte[] dstData){
        int sampleSize = srcData.length;

        int spaceLeft = CONSTANTS.FEC_DATA_LENGTH - dstDataIndex;

        int endIndex = 0;
        int startIndex = 0;

        //If we've got enough space for the whole rest of the frame, then use the rest of the frame
        if(spaceLeft > (sampleSize - mDataIndex)){
            startIndex = mDataIndex;
            if(mDataIndex != 0){
                srcData = Arrays.copyOfRange(srcData, mDataIndex, sampleSize);
                mDataIndex = 0;
            }
            mCurrentFrame++;

        } else {
            //If not, then let's put what we can
            endIndex = spaceLeft + mDataIndex;

            startIndex = mDataIndex;
            //If the space left in the ByteBuffer is greater than the space left in the frame, then just put what's left
            if(endIndex >= srcData.length){
                endIndex = srcData.length;
                mCurrentFrame++;
                mDataIndex = 0;
            } else {
                mDataIndex = endIndex;
            }

            srcData = Arrays.copyOfRange(srcData, startIndex , endIndex);
        }

        //Lets assign the data to the destination array
        while(startIndex < endIndex){
            dstData[dstDataIndex] = srcData[startIndex];
            startIndex++;
            dstDataIndex++;
        }

        return srcData.length;
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
        mBroadcaster = null;
    }

}
