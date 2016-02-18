package io.unisong.android.network.client.receiver;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.emitter.Emitter;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.song.Song;
import io.unisong.android.audio.song.UnisongSong;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.client.Listener;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.CurrentUser;

/**
 * Created by ezturner on 6/8/2015.
 */
public class ServerReceiver implements Receiver{

    private final static String LOG_TAG = ServerReceiver.class.getSimpleName();

    private SocketIOClient client;
    private Listener listener;
    private boolean isConfigured = false;

    public ServerReceiver(Listener listener){
        // Configure Socekt.IO client

        configureSocketIO();

        this.listener = listener;
    }

    private void configureSocketIO(){
        if(isConfigured)
            return;

        isConfigured = true;
        client = SocketIOClient.getInstance();

        // TODO : check for nullpointerexception possibility here
        client.setServerReceiver(this);

        client.on("data", dataListener);
        client.on("start song", songStartListener);
        client.on("pause", pauseListener);
        client.on("add song", addSongListener);
        client.on("delete song", deleteSongListener);
        client.on("end song", endSongListener);
        client.on("end session", endSessionListener);
        client.on("seek", seekListener);
        client.on("resume", resumeListener);
        client.on("play", mPlayListener);
        client.on("update song", updateSongListener);
    }

    public void disconnectSocketIO(){
        if(!isConfigured)
            return;

        isConfigured = false;

        // TODO : check for nullpointerexception possibility here
        client.setServerReceiver(null);

        client.off("data", dataListener);
        client.off("start song", songStartListener);
        client.off("pause", pauseListener);
        client.off("add song", addSongListener);
        client.off("delete song", deleteSongListener);
        client.off("end song", endSongListener);
        client.off("end session", endSessionListener);
        client.off("seek", seekListener);
        client.off("resume", resumeListener);
        client.off("play", mPlayListener);
        client.off("update song", updateSongListener);
    }

    /**
     * The listener for play events
     */
    private Emitter.Listener mPlayListener = new Emitter.Listener(){

        @Override
        public void call(Object... args) {
            listener.play();
        }
    };


    private boolean firstDataReceived = false;
    private long firstDataReceivedTime;
    private int dataReceived = 0;
    private int count = 0;

    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener dataListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            dataReceived++;
            count++;
            if(!firstDataReceived){
                firstDataReceived = true;
                firstDataReceivedTime = System.currentTimeMillis();
            } else if(count >= 100){
                long timeSince = System.currentTimeMillis() - firstDataReceivedTime;
                double avgTime = timeSince / dataReceived;
                count = 0;

                Log.d(LOG_TAG, "Average time per frame: " + avgTime + "ms, at " + dataReceived + " frames received.");
            }

            JSONObject object = (JSONObject) args[0];
            AudioFrame frame;
            try{
                int dataid = object.getInt("dataID");
                int songid = object.getInt("songID");
                byte[] data = (byte[]) object.get("data");

                frame = new AudioFrame(data, dataid, songid);
            } catch (JSONException e){
                e.printStackTrace();
                return;
            }


            listener.addFrame(frame);
        }

    };

    private Emitter.Listener pauseListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "Pause received from server.");
            listener.pause();
        }
    };

    private Emitter.Listener songStartListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {

            try {
                Log.d(LOG_TAG, "Server Song Start received.");
                JSONObject object = (JSONObject) args[0];
                long songStartTime;
                int songID;
                try {
                    songStartTime = object.getLong("songStartTime");
                    songID = object.getInt("songID");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                listener.startSong(songStartTime, songID);
            } catch (Exception e){
                e.printStackTrace();
            }

        }
    };

    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener endSongListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            int songID = (Integer) args[0];
            listener.endSong(songID);
        }
    };

    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener endSessionListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            UnisongSession session = CurrentUser.getInstance().getSession();

            if(session != null){
                session.endSession();
            }
        }
    };


    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener seekListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            // the first arg should be a long with the seek time
            try {
//                Long seekTime = (Long) args[0];
                // seekTime would be a long but socket.io casts it to an int
                int seekTime = (Integer) args[0];
                listener.seek(seekTime);

            }catch(Exception e){
                e.printStackTrace();
            };
        }
    };


    /**
     * The listener for when we receive a Resume event
     *
     * params
     *
     * the new song start time - ms
     *
     */
    private Emitter.Listener resumeListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            try{
                // Get the resumeTime and the new songStartTime
                JSONObject resumeObject = (JSONObject) args[0];

                // update them
                long resumeTime = resumeObject.getLong("resumeTime");
                Long newSongStartTime = resumeObject.getLong("songStartTime");
                listener.resume(resumeTime, newSongStartTime);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    /**
     * The socket.IO listener for when a song is added.
     */
    private Emitter.Listener addSongListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "add song received from server");
            try{

                JSONObject object = (JSONObject) args[0];
                String type = object.getString("type");
                if(type.equals(UnisongSong.TYPE_STRING)){
                    UnisongSong song = new UnisongSong(object);
                    listener.addSong(song);
                }
                // TODO : when added add SoundcloudSong and Spotify/Google play songs.
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    /**
     *
     */
    private Emitter.Listener deleteSongListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "Delete song received");
            try{
                int songID = (Integer) args[0];

                listener.deleteSong(songID);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private Emitter.Listener updateSongListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                JSONObject object = (JSONObject) args[1];
                listener.updateSong(object);
            } catch (ClassCastException e){
                Log.d(LOG_TAG , "UpdateSongListener threw ClassCastException!");
            } catch (NullPointerException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "UpdateSongListener threw NullPointerException!");
            }
        }
    };

    /**
     * This will join the specified Server session.
     * @param sessionID
     */
    public void joinSession(int sessionID){
        client.joinSession(sessionID);
    }

    @Override
    public void requestData(Song songToRequest, int startRange, int endRange) {
        Log.d(LOG_TAG, "Requesting from " + startRange + " to " + endRange);
        try {
            JSONObject requestObject = new JSONObject();
            requestObject.put("startDataID", startRange);
            requestObject.put("endDataID", endRange);
            requestObject.put("songID" , songToRequest.getID());
            requestObject.put("sessionID" , songToRequest.getSessionID());

            client.emit("request data range", requestObject);
        } catch (JSONException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "JSONException in requestData!");
        }

    }

    public void destroy(){
        disconnectSocketIO();
    }
}
