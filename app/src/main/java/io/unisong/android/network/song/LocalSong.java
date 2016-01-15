
package io.unisong.android.network.song;

import android.media.MediaFormat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.unisong.android.activity.session.musicselect.MusicDataManager;
import io.unisong.android.activity.session.musicselect.UISong;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.master.AACEncoder;
import io.unisong.android.audio.master.FileDecoder;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.TimeManager;
import io.unisong.android.network.session.UnisongSession;

/**
 * Created by Ethan on 10/3/2015.
 */
public class LocalSong extends Song {

    private final static String LOG_TAG = LocalSong.class.getSimpleName();

    public final static String TYPE_STRING = "LocalSong";
    private String mPath;
    private FileDecoder mDecoder;
    private AACEncoder mEncoder;
    private SongFormat mFormat;
    private int mSessionID;
    private long mSongStartTime;
    private boolean mStarted;

    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     *
     * @param songJSON - the JSON representation of the song
     */
    public LocalSong(JSONObject songJSON) throws JSONException, NullPointerException{
        super(songJSON.getString("name"), songJSON.getString("artist"), songJSON.getInt("songID")
                ,MusicDataManager.getInstance().getSongImagePathByJSON(songJSON));
        UISong uiSong = null;
        try{
            uiSong = MusicDataManager.getInstance().getSongByJSON(songJSON);
        } catch (JSONException e){
            e.printStackTrace();
        }

        mPath = uiSong.getPath();
        mStarted = false;
        mSessionID = songJSON.getInt("sessionID");
        Log.d(LOG_TAG, "LocalSong created, songID is : " + mSongID);
        Log.d(LOG_TAG , "SongID :" + songJSON.getInt("songID"));
        mEncoder = new AACEncoder();
        mEncoder.setSong(this);
        mDecoder = new FileDecoder(mPath);

        if(songJSON.has("format")) {
            mFormat = new SongFormat(songJSON.getJSONObject("format"));
        } else {
            mFormat = new SongFormat(mPath);
        }
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
        mFormat = new SongFormat(mPath);
    }

    public void setFormat(MediaFormat format){
        mFormat = new SongFormat(format);

        SocketIOClient client = SocketIOClient.getInstance();

        if(client != null)
            client.emit("update song" , toJSON());
    }


    public long getDuration(){
        if(mFormat != null)
            return mFormat.getDuration();
        return -1l;
    }

    @Override
    public String getImageURL() {
        if(mImageURL != null)
            return mImageURL;

        return NetworkUtilities.HTTP_URL + "/session/" + mSessionID + "/songID/" + mSongID + "/picture";
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
        if(!mStarted) {
            mSongStartTime = TimeManager.getInstance().getSongStartTime();
            mDecoder.startDecode();
            mEncoder.encode(0, super.getID(), mPath);
            mStarted = true;
        }
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

            object.put("name" , getName());
            object.put("artist" , getArtist());
            object.put("sessionID" , mSessionID);
            object.put("songID" , mSongID);

            if(mSongStartTime != 0l)
                object.put("songStartTime" , mSongStartTime);

            if(getFormat() != null)
                object.put("format", getFormat().toJSON());

            object.put("type" , UnisongSong.TYPE_STRING);
        } catch (JSONException e){
            e.printStackTrace();
        }

        return object;
    }

    @Override
    public void update(JSONObject songJSON) {

    }

}
