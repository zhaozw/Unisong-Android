package com.ezturner.speakersync.audio;

import com.ezturner.speakersync.Decoder;
import com.ezturner.speakersync.network.AudioBroadcaster;

import java.io.File;
import java.io.IOException;

/**
 * Created by Ethan on 2/12/2015.
 */
public class AudioFileReader {

    //The current file that is being read from.
    private File mCurrentFile;

    //The LAME decoder interface object
    private Decoder mDecoder;

    //The object that broadcasts audio frames
    private AudioBroadcaster mBroadcaster;


    public AudioFileReader(AudioBroadcaster broadcaster){
        mDecoder = new Decoder(mCurrentFile);

        mBroadcaster = broadcaster;

        try {
            mDecoder.initialize();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void readFile(String path) throws IOException{

    }

    private Thread decode = new Thread(new Runnable()  {
        public void run() {
            try {
                startDecode();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    });

    private void startDecode() throws IOException{


        short[] data;

        //MediaPlayer mp = new MediaPlayer();
        //mp.setDataSource(this , Uri.fromFile(mCurrentFile));
        //int duration = mp.getDuration();

        boolean stopCode = false;
        boolean notDone = true;
        int counter = 0;
        while(!mDecoder.streamIsEmpty() || !stopCode){
            data = mDecoder.decodeOneFrame();
            if(data.length == 0){
                stopCode = true;
            } else {
                //mAudioTrack.write(data, 0, data.length); Write the data to wherever
            }

            counter++;
            if(counter >= 550){
                System.gc();
                counter = 0;
            }

        }

        stopCode = false;

    }

}
