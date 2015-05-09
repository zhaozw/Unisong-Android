package com.ezturner.speakersync.network.packets.tcp;

import com.ezturner.speakersync.network.CONSTANTS;
import com.ezturner.speakersync.network.NetworkUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Ethan on 5/8/2015.
 */
public class TCPResumePacket {
    //The time that all of the hosts will pause at
    private long mResumeTime;
    private long mNewSongStartTime;

    public TCPResumePacket(InputStream stream){
        receive(stream);
    }

    public static void send(OutputStream stream, long resumeTime, long newSongStartTime) {
        byte[] data = ByteBuffer.allocate(8).putLong(resumeTime).array();

        byte[] songStartTimeArr = ByteBuffer.allocate(8).putLong(newSongStartTime).array();

        data = NetworkUtilities.combineArrays(data , songStartTimeArr);

        synchronized (stream) {
            try {
                stream.write(CONSTANTS.TCP_PAUSE);
                stream.write(data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void receive(InputStream stream){
        byte[] data = new byte[16];

        synchronized (stream) {
            try {
                stream.read(data);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        mResumeTime = ByteBuffer.wrap(Arrays.copyOfRange(data , 0 , 8)).getLong();
        mNewSongStartTime = ByteBuffer.wrap(Arrays.copyOfRange(data , 8 , 16)).getLong();

    }

    public long getResumeTime(){
        return mResumeTime;
    }

    public long getNewSongStartTime(){
        return mResumeTime;
    }
}
