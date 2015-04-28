package com.ezturner.speakersync.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by Ethan on 4/25/2015.
 */
public class Mp3Reader {

    private File mFile;
    private FileInputStream mInputStream;
    private int mSampleRate;
    private int mChannels;

    public Mp3Reader(String filepath){
        mFile = new File(filepath);
        try {
            mInputStream = new FileInputStream(mFile);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public byte[] readFrame(){
        byte[] header = new byte[4];

        try {
            mInputStream.read(header);
        } catch(IOException e){
            e.printStackTrace();
        }


        return new byte[0];
    }
}
