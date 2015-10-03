package io.unisong.android.network.song;

import android.media.MediaFormat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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

    public SongFormat(MediaFormat format){
        try{
            mMime = format.getString(MediaFormat.KEY_MIME);
            mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            // if duration is 0, we are probably playing a live stream
            mDuration = format.getLong(MediaFormat.KEY_DURATION);
            mBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
        } catch (Exception e){
            e.printStackTrace();
            Log.d(LOG_TAG, "Creation of SongFormat failed.");
        }
    }

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
