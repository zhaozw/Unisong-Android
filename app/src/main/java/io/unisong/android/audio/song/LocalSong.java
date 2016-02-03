
package io.unisong.android.audio.song;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaFormat;
import android.util.Base64;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.unisong.android.activity.session.musicselect.UISong;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.MusicDataManager;
import io.unisong.android.audio.decoder.FileDecoder;
import io.unisong.android.audio.encoder.AACEncoder;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.http.HttpClient;
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
        if (!started) {
            started = true;
            encoder = new AACEncoder(this);
            decoder = new FileDecoder(path);

            ((FileDecoder) decoder).registerSong(this);

            songStartTime = TimeManager.getInstance().getSongStartTime();
            decoder.start();
            encoder.encode(0, path);
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
        ((FileDecoder)decoder).registerSong(this);
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

    @Override
    public void destroy() {
        decoder.destroy();
        encoder.destroy();
        decoder = null;
        encoder = null;
    }

    // TODO : see if we need to do anything special for this.
    public void endSong(){

    }

    /**
     * Uploads the song picture to the server so that the clients
     * can see it
     */
    private void uploadPicture(){

        if(imageURL == null || imageURL.equals(""))
            return;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(getImageURL(), options);
        int smallest = 120;

        options.inSampleSize = calculateInSampleSize(options,120, 120);

        options.inJustDecodeBounds = false;
        uploadSongPicture(BitmapFactory.decodeFile(imageURL, options));

    }

    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private void uploadSongPicture(Bitmap bitmap) throws NullPointerException{
        // TODO : don't use bitmaps?
        Log.d(LOG_TAG, "Bitmap size: " + bitmap.getByteCount() + " bytes.");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        } catch (NullPointerException e){
            e.printStackTrace();
            return;
        }

        byte[] byteArray = byteArrayOutputStream .toByteArray();
        Log.d(LOG_TAG , "Byte array size: " + byteArray.length + " bytes.");
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        Log.d(LOG_TAG , "Encoded getBytes() size : " + encoded.getBytes().length + " bytes.");

        uploadPicture(encoded);
    }


    private void uploadPicture(String encoded){
        JSONObject object = new JSONObject();
        try {
            object.put("data", encoded);
        } catch (JSONException e){
            e.printStackTrace();
            return;
        }

        Callback uploadCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
                // TODO : implement?
            }

            @Override
            public void onResponse(Response response) throws IOException {
                // TODO : implement?
            }
        };

        try {
            HttpClient.getInstance().post(NetworkUtilities.HTTP_URL + "/session/" + sessionID + "/song/" + songID + "/picture", object, uploadCallback);
        } catch (IOException e){
            e.printStackTrace();
        }

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
