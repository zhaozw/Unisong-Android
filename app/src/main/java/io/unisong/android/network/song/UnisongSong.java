package io.unisong.android.network.song;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.client.SongDecoder;

/**
 * This class handles songs that are broadcasted over the Unisong network.
 * Created by Ethan on 10/3/2015.
 */
public class UnisongSong extends Song {

    private final static String LOG_TAG = UnisongSong.class.getSimpleName();
    public final static String TYPE_STRING = "UnisongSong";

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
    public UnisongSong(String name, String artist, long duration,int ID ,  String imageURL, SongFormat inputFormat) {
        super(name, artist,ID ,  imageURL);
        mFormat = inputFormat;
        Log.d(LOG_TAG, mFormat.toString());
        mSongDecoder = new SongDecoder(inputFormat);
    }

    public UnisongSong(JSONObject object) throws JSONException{
        super(object.getString("name"), object.getString("artist"), object.getInt("ID"), object.getString("imageURL"));
        mFormat = new SongFormat(object.getJSONObject("format"));
        Log.d(LOG_TAG, mFormat.toString());
        mSongDecoder = new SongDecoder(getFormat());
    }

    // TODO : write and actually implement.
    public long getDuration(){
        return 0l;
    }
    @Override
    public AudioFrame getFrame(int ID) {
        return mSongDecoder.getFrame(ID);
    }

    @Override
    public AudioFrame getPCMFrame(int ID) {
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

    /**
     * Starts the decoding of the song.
     */
    @Override
    public void start() {
        mSongDecoder.decode(0);
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


}
