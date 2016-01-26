package io.unisong.android.network.song;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.client.SongDecoder;
import io.unisong.android.network.NetworkUtilities;

/**
 * This class handles songs that are broadcasted over the Unisong network.
 * Created by Ethan on 10/3/2015.
 */
public class UnisongSong extends Song {

    private final static String LOG_TAG = UnisongSong.class.getSimpleName();
    public final static String TYPE_STRING = "UnisongSong";

    private int mSessionID;
    private SongFormat mFormat;
    private SongDecoder mSongDecoder;

    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     *
     * @param name
     * @param artist
     * @param duration
     * @param imageURL
     */
    public UnisongSong(String name, String artist, long duration,int ID ,  String imageURL, SongFormat inputFormat, int sessionID) {
        super(name, artist,ID ,  imageURL);
        mFormat = inputFormat;
        mSessionID = sessionID;
        Log.d(LOG_TAG, mFormat.toString());
    }

    public UnisongSong(JSONObject object) throws JSONException{
        // TODO : fix the imageURL stuff
        super(object.getString("name"), object.getString("artist"), object.getInt("songID"), null);//object.getString("imageURL"));

        if(object.has("format")) {
            mFormat = new SongFormat(object.getJSONObject("format"));
            Log.d(LOG_TAG, mFormat.toString());
        }

        mSongID = object.getInt("songID");
        mSessionID = object.getInt("sessionID");

    }

    public long getDuration(){
        SongFormat format = getFormat();
        if(format != null)
            return getFormat().getDuration();

        return -1;
    }

    @Override
    public AudioFrame getFrame(int ID) {
        return mSongDecoder.getFrame(ID);
    }

    @Override
    public synchronized AudioFrame getPCMFrame(int ID) {
        return mSongDecoder.getFrame(ID);
    }

    @Override
    public boolean hasFrame(int ID) {
        return mSongDecoder.hasInputFrame(ID);
    }

    @Override
    public boolean hasPCMFrame(int ID) {
        return mSongDecoder.hasFrame(ID);
    }

    public String getImageURL(){
        return NetworkUtilities.HTTP_URL + "/session/" + mSessionID + "/song/" + mSongID +"/picture";
    }
    /**
     * Starts the decoding of the song.
     */
    @Override
    public void start() {
        if(!mStarted){
            mSongDecoder = new SongDecoder(getFormat());
            mSongDecoder.decode(0);
            mStarted = true;
        }
    }

    @Override
    public void seek(long seekTime) {
        mSongDecoder.seek(seekTime);
    }

    @Override
    public Map<Integer, AudioFrame> getPCMFrames() {
        return mSongDecoder.getFrames();
    }

    @Override
    public SongFormat getFormat() {
        return mFormat;
    }

    @Override
    public void addFrame(AudioFrame frame) {
        mSongDecoder.addInputFrame(frame);
    }

    public JSONObject toJSON(){
        JSONObject object = new JSONObject();

        try {
            object.put("name" , getName());
            object.put("artist" , getArtist());
            if(getFormat() != null)
                object.put("format", getFormat().toJSON());
            object.put("type" , TYPE_STRING);
            object.put("songID" , mSongID);
            object.put("sessionID" , mSessionID);

        } catch (JSONException e){
            e.printStackTrace();
        }

        return object;
    }

    // TODO : figure out a situation where this must be updated?
    @Override
    public void update(JSONObject songJSON) {
        try {
            if (songJSON.has("format") && mFormat == null)
                mFormat = new SongFormat(songJSON.getJSONObject("format"));
        } catch (JSONException e){
            e.printStackTrace();
        }

    }

}
