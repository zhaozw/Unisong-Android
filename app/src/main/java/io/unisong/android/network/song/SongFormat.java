package io.unisong.android.network.song;

import android.media.MediaFormat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * This handles the format of a given song's transmitted data. Currently only created fron UISong
 * and used in UnisongSong
 * Created by Ethan on 10/3/2015.
 */
public class SongFormat {

    private final static String LOG_TAG = SongFormat.class.getSimpleName();

    private int mBitrate;
    private String mMime;
    private long mDuration;
    private int mSampleRate;
    private int mChannels;

    /**
     * Creates a song format from a MediaFormat
     *
     * @param format the MediaFormat used for instantiation
     */
    public SongFormat(MediaFormat format){
        try{
            if(format.containsKey(MediaFormat.KEY_MIME))
                mMime = format.getString(MediaFormat.KEY_MIME);
            if(format.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            if(format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            // if duration is 0, we are probably playing a live stream
            if(format.containsKey(MediaFormat.KEY_DURATION))
                mDuration = TimeUnit.MICROSECONDS.convert(format.getLong(MediaFormat.KEY_DURATION) , TimeUnit.MILLISECONDS);
            if(format.containsKey(MediaFormat.KEY_BIT_RATE))
                mBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
        } catch (Exception e){
            e.printStackTrace();
            Log.d(LOG_TAG, "Creation of SongFormat failed.");
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
                mBitrate = object.getInt("bitrate");

            if(object.has("mime"))
                mMime = object.getString("mime");

            if(object.has("duration"))
                mDuration = object.getLong("duration");

            if(object.has("sample_rate"))
                mSampleRate = object.getInt("sample_rate");

            if(object.has("channels"))
                mChannels = object.getInt("channels");

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
            object.put("mime", mMime);
            object.put("bitrate" , mBitrate);
            object.put("sample_rate" , mSampleRate);
            object.put("channels" , mChannels);
            object.put("duration" , mDuration);
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
        return mBitrate;
    }

    public String getMime(){
        return mMime;
    }

    public long getDuration(){
        return mDuration;
    }

    public int getSampleRate(){
        return mSampleRate;
    }

    public int getChannels(){
        return mChannels;
    }
}
