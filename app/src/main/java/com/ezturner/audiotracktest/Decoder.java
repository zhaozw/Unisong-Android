package com.ezturner.audiotracktest;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Decoder {
    private static final int DEFAULT_FRAME_SIZE = 1152;
    private static final int INPUT_STREAM_BUFFER = 8192;
    private static final int MP3_SAMPLE_DELAY = 528;
    private static final int MP3_ENCODER_DELAY = 576;
    //private WaveWriter waveWriter;
    private File inFile;
    private File outFile;

    private BufferedInputStream in;

    public Decoder(File inFile) {
        this.inFile = inFile;
    }

    public void initialize() throws IOException {
        in = new BufferedInputStream(new FileInputStream(inFile),
                INPUT_STREAM_BUFFER);
        lameInitialize(in);

    }

    private static int lameInitialize(BufferedInputStream in) throws IOException {
        int ret = 0;
        ret = Lame.initializeDecoder();
        ret = Lame.configureDecoder(in);
        return ret;
    }



    public short[] decode() throws IOException {
        if (/* waveWriter != null && */ in != null) {

            int samplesRead = 0, offset = 0;
            int skip_start = 0, skip_end = 0;
            int delay = Lame.getDecoderDelay();
            int padding = Lame.getDecoderPadding();
            int frameSize = Lame.getDecoderFrameSize();
            int totalFrames = Lame.getDecoderTotalFrames();



            int frame = 0;
            short[] leftBuffer = new short[DEFAULT_FRAME_SIZE];
            short[] rightBuffer = new short[DEFAULT_FRAME_SIZE];

            short[] result = new short[0];

            if (delay > -1 || padding > -1) {
                if (delay > -1) {
                    skip_start = delay + (MP3_SAMPLE_DELAY + 1);
                }
                if (padding > -1) {
                    skip_end = padding - (MP3_SAMPLE_DELAY + 1);
                }
            } else {
                skip_start = MP3_ENCODER_DELAY + (MP3_SAMPLE_DELAY + 1);
            }

            while(true){
                samplesRead = Lame.decodeFrame(in, leftBuffer, rightBuffer);
                offset = skip_start < samplesRead ? skip_start : samplesRead;
                skip_start -= offset;
                frame += samplesRead / frameSize;
                if (samplesRead >= 0) {
                    if (skip_end > DEFAULT_FRAME_SIZE && frame + 2 > totalFrames) {
                        samplesRead -= (skip_end - DEFAULT_FRAME_SIZE);
                        skip_end = DEFAULT_FRAME_SIZE;
                    } else if (frame == totalFrames && samplesRead == 0) {
                        samplesRead -= skip_end;
                    }

                    if (Lame.getDecoderChannels() == 2) {
                        short[] combined = merge(leftBuffer, rightBuffer);

                        result = combineArrays(result, combined);

                        //waveWriter.write(leftBuffer, rightBuffer, offset, samplesRead);
                    } else {
                        result = combineArrays(result, leftBuffer);
                        //waveWriter.write(leftBuffer, offset, samplesRead);
                    }

                } else {
                    break;
                }
            }

            return result;
        }
        return null;
    }

    public short[] decodeOneFrame() throws IOException{
        int samplesRead = 0, offset = 0;
        int skip_start = 0, skip_end = 0;
        int delay = Lame.getDecoderDelay();
        int padding = Lame.getDecoderPadding();
        int frameSize = Lame.getDecoderFrameSize();
        int totalFrames = Lame.getDecoderTotalFrames();



        int frame = 0;
        short[] leftBuffer = new short[DEFAULT_FRAME_SIZE];
        short[] rightBuffer = new short[DEFAULT_FRAME_SIZE];

        short[] result = new short[0];

        if (delay > -1 || padding > -1) {
            if (delay > -1) {
                skip_start = delay + (MP3_SAMPLE_DELAY + 1);
            }
            if (padding > -1) {
                skip_end = padding - (MP3_SAMPLE_DELAY + 1);
            }
        } else {
            skip_start = MP3_ENCODER_DELAY + (MP3_SAMPLE_DELAY + 1);
        }


        samplesRead = Lame.decodeFrame(in, leftBuffer, rightBuffer);
        if(samplesRead  <= 0){
            return new short[0];
        }
        offset = skip_start < samplesRead ? skip_start : samplesRead;
        skip_start -= offset;
        frame += samplesRead / frameSize;
        if (skip_end > DEFAULT_FRAME_SIZE && frame + 2 > totalFrames) {
            samplesRead -= (skip_end - DEFAULT_FRAME_SIZE);
            skip_end = DEFAULT_FRAME_SIZE;
        } else if (frame == totalFrames && samplesRead == 0) {
            samplesRead -= skip_end;
        }

        if (Lame.getDecoderChannels() == 2) {
            return merge(leftBuffer, rightBuffer);

            //waveWriter.write(leftBuffer, rightBuffer, offset, samplesRead);
        } else {
            return leftBuffer;
            //waveWriter.write(leftBuffer, offset, samplesRead);
        }



    }

    short[] merge(short[] a, short[] b)
    {
        assert (a.length == b.length);

        short[] result = new short[a.length + b.length];

        for (int i=0; i<a.length; i++)
        {
            result[i*2] = a[i];
            result[i*2+1] = b[i];
        }

        return result;
    }

    short[] combineArrays(short[] a, short[] b){
        int aLen = a.length;
        int bLen = b.length;
        short[] c= new short[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public void cleanup() {
        try {
            //if (waveWriter != null) {
            //    waveWriter.closeWaveFile();
            //}
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            // failed to close mp3 file or close output file
            // TODO: actually handle an error here
            e.printStackTrace();
        }
        Lame.closeDecoder();
    }

    public boolean streamIsEmpty(){
        try {
            Log.d("ezturner" , "Available :" + in.available());
            return in.available() == 0;
        } catch(IOException e){
            e.printStackTrace();
            return false;
        }
    }

}
