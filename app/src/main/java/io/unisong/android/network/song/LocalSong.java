
package io.unisong.android.network.song;

import android.media.MediaFormat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.unisong.android.activity.musicselect.UISong;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.master.AACEncoder;
import io.unisong.android.audio.master.FileDecoder;
import io.unisong.android.network.SocketIOClient;

/**
 * Created by Ethan on 10/3/2015.
 */
public class LocalSong extends Song {

    private String mPath;
    private FileDecoder mDecoder;
    private AACEncoder mEncoder;
    private SongFormat mFormat;
    private boolean mStarted;
    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     *
     * @param name
     * @param artist
     * @param imageURL
     */
    public LocalSong(String name, String artist, int ID , String imageURL, String path) {
        super(name, artist, ID, imageURL);
        mPath = path;
        mStarted = false;
    }

    /**
     * This constructor takes in a UISong and creates a LocalSong from it.
     * @param uiSong
     */
    public LocalSong(UISong uiSong){
        super(uiSong.getName() , uiSong.getArtist() , uiSong.getImageURL());
        mPath = uiSong.getPath();
        mEncoder = new AACEncoder();
        mEncoder.setSong(this);
        mDecoder = new FileDecoder(mPath);
        start();
    }

    private Runnable mCreateRunnable = new Runnable() {
        @Override
        public void run() {
            SocketIOClient client = SocketIOClient.getInstance();
            client.emit("add song" , toJSON());
        }
    };


    public void setFormat(MediaFormat format){
        mFormat = new SongFormat(format);
    }


    public long getDuration(){
        if(mFormat != null)
            return mFormat.getDuration();
        return -1l;
    }

    /**
     * Returns an encoded frame.
     * @param ID
     * @return
     */
    public AudioFrame getFrame(int ID) {
        return mEncoder.getFrame(ID);
    }

    /**
     * Returns the PCM frame with the specified ID
     * @param ID - The ID of the given frame
     * @return
     */
    @Override
    public AudioFrame getPCMFrame(int ID) {
        return mDecoder.getFrame(ID);
    }

    /**
     * Begins the PCM decoding and AAC encoding.
     */
    public void start(){
        mDecoder.startDecode();
        mEncoder.encode(0 , super.getID() , mPath);
    }

    public boolean hasFrame(int ID){
        return mEncoder.hasFrame(ID);
    }

    public boolean hasPCMFrame(int ID){
        return mDecoder.hasFrame(ID);
    }

    public void seek(long seekTime){
        mDecoder.seek(seekTime);
        mEncoder.seek(seekTime);
    }

    @Override
    public Map<Integer, AudioFrame> getPCMFrames() {
        return mDecoder.getFrames();
    }

    @Override
    public SongFormat getFormat() {
        return mFormat;
    }

    @Override
    public void addFrame(AudioFrame frame) {

    }

    // TODO: make toJSON with the data standard/update docs with new stuff
    public JSONObject toJSON(){
        JSONObject object = new JSONObject();

        try {

            object.put("format", getFormat().toJSON());

        } catch (JSONException e){
            e.printStackTrace();
        }

        return object;
    }

}
