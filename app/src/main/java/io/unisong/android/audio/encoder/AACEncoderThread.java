package io.unisong.android.audio.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Base64;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.decoder.FileDecoder;
import io.unisong.android.audio.song.LocalSong;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.session.UnisongSession;

/**
 * This class represents a single Thread running in the AACEncoder class
 * This has its own class to prevent issues with starting and stopping threads.
 * Created by ezturner on 2/1/2016.
 */
public class AACEncoderThread extends Thread {

    private static final String LOG_TAG = AACEncoderThread.class.getSimpleName();

    private boolean running = false;
    private int dataIndex = 0;

    private long startTime;

    // The input frames in PCM and the output frames in AAC
    private Map<Integer, AudioFrame> inputFrames, outputFrames;

    private int currentInputID, currentOutputID;
    private int frameBufferSize = 50, songID;

    private String filePath;
    private LocalSong song;


    long presentationTimeUs = 0;

    private FileDecoder decoder;
    private MediaCodec codec;

    /**
     * Create an AACEncoderThread instance to handle the encoding and
     * of a given song
     * @param outputFrames the map of output frames to be shared with the AACEncoder
     * @param songID the songID of the song to encode (for the AudioFrame)
     * @param filePath the path of the file to encode from
     */
    public AACEncoderThread(Map<Integer, AudioFrame> outputFrames,
                            int songID , String filePath){
        this.outputFrames = outputFrames;
        this.songID = songID;
        this.filePath = filePath;
        inputFrames = new HashMap<>();
    }

    /**
     * Sets the buffer size in number of frames
     * @param size the number of frames to keep in the buffer
     */
    public void setFrameBufferSize(int size){
        frameBufferSize = size;
    }

    /**
     * Begins the encoding process and starts a thread.
     */
    public void startEncode(){
        currentInputID = 0;
        decoder = new FileDecoder(filePath);
        // set the buffer size to larger than usual to ensure there is no latency
        decoder.setFrameBufferSize(100);

        // begin the decoder so we have PCM data
        decoder.start(startTime);
        Log.d(LOG_TAG , "Starting for path : " + filePath);

        // start the thread
        this.start();
    }

    /**
     * Begins the encoding process at a given time
     * @param time the time to begin at in microseconds
     */
    public void startEncode(long time){
        startTime = time;
        currentOutputID = (int) (startTime / (1024000.0 / 44100.0));
        startEncode();
    }

    @Override
    public void run(){
        running = true;
        encode();
        running = false;
    }

    /**
     * Begins the process of encoding the AAC data.
     */
    private void encode(){
        boolean firstData = true;
        // TODO : get rid of frames that have been played.
        long startTime = System.currentTimeMillis();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        Log.d(LOG_TAG, "Creating codec : OMX.google.aac.encoder");
        // create the actual decoder, using the mime to select
        try{
            //TODO: see if this works on all devices.
            codec = MediaCodec.createEncoderByType(AACEncoder.MIME);
        } catch(IOException e){
            e.printStackTrace();
        }

        // ensure that we have a valid codec instance
        if (codec == null){
            Log.d(LOG_TAG , "codec is null ):");
            return;
        }


        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, AACEncoder.MIME);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC); //fixed version
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, AACEncoder.SAMPLE_RATE);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64000 * AACEncoder.CHANNELS);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, AACEncoder.CHANNELS);

        //song.setFormat(format);

        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        codec.start();
        ByteBuffer[] codecInputBuffers  = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

        // start decoding
        final long kTimeOutUs = 1000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        Log.d(LOG_TAG , "Info size is : "  + info.size);
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;


        while(running){

            // wait while we do not have the input frame
            if(!inputFrames.containsKey(currentInputID)) {
                boolean firstWait = true;
                while (!decoder.hasOutputFrame(currentInputID)) {
                    if(firstWait) {
                        Log.d(LOG_TAG , "waiting for frame #" + currentInputID);
                        firstWait = false;
                    }

                    if (!running) {
                        break;
                    }

                    try {
                        synchronized (this) {
                            this.wait(10);
                        }
                    } catch (InterruptedException e) {
                        Log.d(LOG_TAG, "Waiting interrupted");
                    }


                }

                inputFrames.put(currentInputID, decoder.getFrame(currentInputID));
            }

            if(!inputFrames.containsKey(currentInputID))
                Log.d(LOG_TAG , "Does not contain key");

            // while our output frames are full, wait.
            while(outputFrames.size() >= frameBufferSize){
                try{
                    synchronized (this){
                        this.wait(10);
                    }
                } catch (InterruptedException e){

                }

            }

            AudioFrame frame = inputFrames.get(currentInputID);

            if(frame == null)
                continue;

            long playTime = -1;
            long length = -1;

            noOutputCounter++;
            // read a buffer before feeding it to the decoder
            int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
            if (inputBufIndex >= 0) {
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                dstBuf.clear();

                int sampleSize = setData(frame, dstBuf);

                codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs,
                        sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

            }


            // encode to AAC and then put in outputFrames
            int outputBufIndex = codec.dequeueOutputBuffer(info, kTimeOutUs);


            if (outputBufIndex >= 0){
                if (info.size > 0)  noOutputCounter = 0;

                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

//                Log.d(LOG_TAG , "Output Buffer limit is : " + buf.limit());
                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                // if this is the first data received, upload the CSD buffers
                if(firstData){
                    uploadCSDBuffer(chunk);
                    firstData = false;
                } else if(chunk.length > 0){
                    // otherwise create a frame
                    createFrame(chunk);
                }

                try {
                    codec.releaseOutputBuffer(outputBufIndex, false);
                } catch(IllegalStateException e){
                    e.printStackTrace();
                }
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.d(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                codecOutputBuffers = codec.getOutputBuffers();
                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                MediaFormat outFormat = codec.getOutputFormat();
                Log.d(LOG_TAG, "output format has changed to " + outFormat);
            } else {
                //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }


        }

        Log.d(LOG_TAG, "stopping...");

        decoder.destroy();

        long finishTime = System.currentTimeMillis();

        Log.d(LOG_TAG , "Total time taken : " + (finishTime - startTime) / 1000.0 + " seconds");
    }

    /**
     * This method will grab data from the AudioFrame, and place it in the dstBuf
     * @param frame - the frame to retrieve data from
     * @param dstBuf - the ByteBuffer to place data into
     * @return
     */
    private int setData(AudioFrame frame , ByteBuffer dstBuf){

        byte[] data = frame.getData();
        int sampleSize = data.length;

        int spaceLeft = dstBuf.capacity() - dstBuf.position();


        //If we've got enough space for the whole rest of the frame, then use the rest of the frame
        if(spaceLeft > (sampleSize - dataIndex) ){
            if(dataIndex != 0) {
                data = Arrays.copyOfRange(data, dataIndex, sampleSize);
                dataIndex = 0;
            }

            inputFrames.remove(frame.getID());
            currentInputID++;

        } else {
            //If not, then let's put what we can
            int endIndex = spaceLeft + dataIndex;

            int startIndex = dataIndex;
            //If the space left in the ByteBuffer is greater than the space left in the frame, then just put what's left
            if(endIndex >= data.length){
                endIndex = data.length;
                currentInputID++;
                inputFrames.remove(frame.getID());
                dataIndex = 0;
            } else {
                dataIndex = endIndex;
            }

            data = Arrays.copyOfRange(data, startIndex , endIndex);
        }

        dstBuf.put(data);

        return data.length;
    }

    /**
     * Signals to the thread that we are done encoding.
     */
    public void stopEncoding(){
        running = false;
    }

    /**
     *
     * @param data
     */
    private void createFrame(byte[] data){
        AudioFrame frame = new AudioFrame(data, currentOutputID, songID);
        currentOutputID++;

        if(currentOutputID % 100 == 0)
            Log.d(LOG_TAG , "Frame #" + currentOutputID + " created");


        if(frame == null){
            Log.d(LOG_TAG , "AudioFrame is Null. It certainly should not be.");
        } else {
            synchronized (outputFrames) {
                outputFrames.put(frame.getID(), frame);
            }
        }

    }

    /**
     * Uploads the CSD(Codec specific data) Buffers to the server for standalone download by the clients.
     * Without the CSD buffers when the client attempts to download the data at some point in the middle
     * of the output it will fail.
     */
    private void uploadCSDBuffer(byte[] buffer){
        HttpClient client = HttpClient.getInstance();
        Log.d(LOG_TAG , "Posting CSD Buffers");

        Callback callback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
                Log.d(LOG_TAG , "Posting CSD Buffers failed");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if(response.code() == 403){
                    client.reauthorize();
                } else if(response.code() == 200){
                    Log.d(LOG_TAG , "CSD Buffers posted correctly.");
                }
            }
        };

        String base64 = Base64.encodeToString(buffer, Base64.DEFAULT);
        JSONObject object = new JSONObject();

        Log.d(LOG_TAG , "URL : " + NetworkUtilities.HTTP_URL + "/session/" +
                UnisongSession.getCurrentSession().getSessionID() + "/song/" + songID + "/CSDBuffer");
        try{
            object.put("buffer" , base64);
            client.post(NetworkUtilities.HTTP_URL + "/session/" +
                    UnisongSession.getCurrentSession().getSessionID() + "/song/" + songID + "/CSDBuffer", object , callback);
        } catch (JSONException e){
            e.printStackTrace();
        }

    }

}
