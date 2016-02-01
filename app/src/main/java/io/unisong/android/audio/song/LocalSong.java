
package io.unisong.android.audio.song;

import android.media.MediaFormat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.unisong.android.activity.session.musicselect.UISong;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.MusicDataManager;
import io.unisong.android.audio.decoder.FileDecoder;
import io.unisong.android.audio.encoder.AACEncoder;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.ntp.TimeManager;
import io.unisong.android.network.session.UnisongSession;

/**
 * A song that is on this device.
 * Created by Ethan on 10/3/2015.
 */
public class LocalSong extends Song {

    private final static String LOG_TAG = LocalSong.class.getSimpleName();

    public final static String TYPE_STRING = "LocalSong";
    private String path;
    private AACEncoder encoder;
    private SongFormat format;
    private int sessionID;
    private long songStartTime;

    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     *
     * @param songJSON - the JSON representation of the song
     */
    public LocalSong(JSONObject songJSON) throws JSONException, NullPointerException{
        super(songJSON.getString("name"), songJSON.getString("artist"), songJSON.getInt("songID")
                ,MusicDataManager.getInstance().getSongImagePathByJSON(songJSON));

        UISong uiSong = MusicDataManager.getInstance().getSongByJSON(songJSON);

        path = uiSong.getPath();
        started = false;
        sessionID = songJSON.getInt("sessionID");
        Log.d(LOG_TAG, "LocalSong created from JSON, songID is : " + songID);
        Log.d(LOG_TAG, "SongID :" + songJSON.getInt("songID"));

        if(songJSON.has("format")) {
            format = new SongFormat(songJSON.getJSONObject("format"));
        } else {
            format = new SongFormat(path);
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
            sessionID = session.getSessionID();
            songID = session.incrementNewSongID();
        } catch (NullPointerException e){
            e.printStackTrace();
            // this will call if the session is either improperly initialized or not at all.
        }
        path = uiSong.getPath();
        format = new SongFormat(path);
        uploadPicture();
    }

    public void setFormat(MediaFormat format){
        this.format = new SongFormat(format);

        SocketIOClient client = SocketIOClient.getInstance();

        if(client != null)
            client.emit("update song" , toJSON());
    }


    public long getDuration(){
        if(format != null)
            return format.getDuration();
        return -1l;
    }

    @Override
    public String getImageURL() {
        if(imageURL != null)
            return imageURL;

        return NetworkUtilities.HTTP_URL + "/session/" + sessionID + "/songID/" + songID + "/picture";
    }

    /**
     * Returns an encoded frame.
     * @param ID
     * @return
     */
    public AudioFrame getAACFrame(int ID) {
        return encoder.getFrame(ID);
    }

    /**
     * Returns the PCM frame with the specified ID
     * @param ID - The ID of the given frame
     * @return
     */
    @Override
    public AudioFrame getPCMFrame(int ID) {
        return decoder.getFrame(ID);
    }

    /**
     * Begins the PCM decoding and AAC encoding.
     */
    public void start(){
        if(!started) {
            encoder = new AACEncoder(this);
            decoder = new FileDecoder(path);

            songStartTime = TimeManager.getInstance().getSongStartTime();
            decoder.start();
            encoder.encode(0, path);
            started = true;
        }
    }

    public boolean hasAACFrame(int ID){
        if(encoder == null)
            return false;

        return encoder.hasFrame(ID);
    }

    public boolean hasPCMFrame(int ID){
        if(decoder == null)
            return false;

        return decoder.hasOutputFrame(ID);
    }

    public void seek(long seekTime){
        decoder = new FileDecoder(path);
        decoder.start(seekTime);
        encoder.seek(seekTime);
    }

    @Override
    public SongFormat getFormat() {
        return format;
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
            object.put("sessionID" , sessionID);
            object.put("songID" , songID);

            if(songStartTime != 0l)
                object.put("songStartTime" , songStartTime);

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

    // TODO : see if we need to do anything special for this.
    public void endSong(){

    }

    /**
     * Uploads the song picture to the server so that the clients
     * can see it
     */
    private void uploadPicture(){

    }

    @Override
    public void update(int state){
        switch(state) {
            case AudioStatePublisher.START_SONG:
                start();
                break;
            case AudioStatePublisher.END_SONG:
                endSong();
                break;
            case AudioStatePublisher.SEEK:
                Log.d(LOG_TAG , "Seek receieved for song #" + songID + " , seeking encoder and decoder.");
                seek(AudioStatePublisher.getInstance().getSeekTime());
                break;
        }
    }

}
