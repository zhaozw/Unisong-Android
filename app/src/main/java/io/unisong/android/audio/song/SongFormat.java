package io.unisong.android.audio.song;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This handles the format of a given song's transmitted data. Currently only created fron UISong
 * and used in UnisongSong
 * Created by Ethan on 10/3/2015.
 */
public class SongFormat {

    private final static String LOG_TAG = SongFormat.class.getSimpleName();

    private int bitrate;
    private String mime;
    private long duration;
    private int sampleRate;
    private int channels;

    /**
     * Creates a song format from a MediaFormat
     *
     * @param format the MediaFormat used for instantiation
     */
    public SongFormat(MediaFormat format){
        try{
            if(format.containsKey(MediaFormat.KEY_MIME))
                mime = format.getString(MediaFormat.KEY_MIME);
            if(format.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            if(format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            // if duration is 0, we are probably playing a live stream
            if(format.containsKey(MediaFormat.KEY_DURATION))
                duration = TimeUnit.MICROSECONDS.convert(format.getLong(MediaFormat.KEY_DURATION) , TimeUnit.MILLISECONDS);
            if(format.containsKey(MediaFormat.KEY_BIT_RATE))
                bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
        } catch (Exception e){
            e.printStackTrace();
            Log.d(LOG_TAG, "Creation of SongFormat failed.");
        }
    }

    // TODO : see if this doesn't work?
    public SongFormat(String path){
        mime = "audio/mp4a-latm";
        channels = 2;
        bitrate = 128000;
        sampleRate = 44100;

        MediaExtractor extractor = new MediaExtractor();
        // try to set the source, this might fail
        try {
            extractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
            //TODO : Handle this exception
            return;
        }

        // Read duration
        try {
            MediaFormat mediaFormat = extractor.getTrackFormat(0);

            // if duration is 0, we are probably playing a live stream
            duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Reading format parameters exception: " + e.getMessage());
            e.printStackTrace();
            // don't exit, tolerate this error, we'll fail later if this is critical
        }

    }

    /**
     * Creates a SongFormat object from a JSON object. Typically used to create
     * a SongFormat for UnisongSong
     * @param object the JSONObject received from the server/another host
     */
    public SongFormat(JSONObject object){
        try{
            if(object.has("birate"))
                bitrate = object.getInt("bitrate");

            if(object.has("mime"))
                mime = object.getString("mime");

            if(object.has("duration"))
                duration = object.getLong("duration");

            if(object.has("sample_rate"))
                sampleRate = object.getInt("sample_rate");

            if(object.has("channels"))
                channels = object.getInt("channels");

        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * Will create a JSON representation of this object for this to be transmitted.
     * @return object - the JSONObject to create
     */
    public JSONObject toJSON(){
        JSONObject object = new JSONObject();
        try {
            object.put("mime", mime);
            object.put("bitrate" , bitrate);
            object.put("sample_rate" , sampleRate);
            object.put("channels" , channels);
            object.put("duration" , duration);
        } catch (JSONException e){
            e.printStackTrace();
        }

        return object;
    }

    /**
     * Getters and setters.
     * @return
     */
    public int getBitrate(){
        return bitrate;
    }

    public String getMime(){
        return mime;
    }

    public long getDuration(){
        return duration;
    }

    public int getSampleRate(){
        return sampleRate;
    }

    public int getChannels(){
        return channels;
    }

    public MediaFormat getMediaFormat(){
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, mime);
        //fixed version
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channels);

        return format;
    }
}
