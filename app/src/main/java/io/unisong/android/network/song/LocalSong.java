
package io.unisong.android.network.song;

import android.media.MediaFormat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

import io.unisong.android.activity.musicselect.UISong;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.master.AACEncoder;
import io.unisong.android.audio.master.FileDecoder;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.session.UnisongSession;

/**
 * Created by Ethan on 10/3/2015.
 */
public class LocalSong extends Song {

    public final static String TYPE_STRING = "LocalSong";
    private String mPath;
    private FileDecoder mDecoder;
    private AACEncoder mEncoder;
    private SongFormat mFormat;
    private String mSessionID;
    private int mSongID;
    private boolean mStarted;
    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     *
     * @param name
     * @param artist
     * @param imageURL
     */
    public LocalSong(String name, String artist, int ID , String imageURL, String path, String sessionID) {
        super(name, artist, ID, imageURL);
        mPath = path;
        mStarted = false;
        mSessionID = sessionID;
    }

    /**
     * This constructor takes in a UISong and creates a LocalSong from it.
     * @param uiSong
     */
    public LocalSong(UISong uiSong){
        super(uiSong.getName() , uiSong.getArtist() , uiSong.getImageURL());
        try {
            UnisongSession session = UnisongSession.getCurrentSession();
            mSessionID = session.getSessionID();
            mSongID = session.incrementNewSongID();
        } catch (NullPointerException e){
            e.printStackTrace();
            // this will call if the session is either improperly initialized or not at all.
        }
        mPath = uiSong.getPath();
        mEncoder = new AACEncoder();
        mEncoder.setSong(this);
        mDecoder = new FileDecoder(mPath);
    }

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
    public JSONObject getJSON(){
        JSONObject object = new JSONObject();

        try {
            object.put("name" , getName());
            object.put("artist" , getArtist());
            object.put("sessionID" , mSessionID);
            object.put("songID" , mSongID);
            if(getFormat() != null)
                object.put("format", getFormat().toJSON());


        } catch (JSONException e){
            e.printStackTrace();
        }

        return object;
    }

}
