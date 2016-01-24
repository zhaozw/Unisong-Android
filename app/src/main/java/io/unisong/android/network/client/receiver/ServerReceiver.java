package io.unisong.android.network.client.receiver;

import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import io.socket.emitter.Emitter;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.network.client.Listener;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.SocketIOClient;
import io.unisong.android.network.song.UnisongSong;
import io.unisong.android.network.user.CurrentUser;

/**
 * Created by ezturner on 6/8/2015.
 */
public class ServerReceiver implements Receiver{

    private final static String LOG_TAG = ServerReceiver.class.getSimpleName();

    private SocketIOClient mClient;
    private Listener mListener;

    public ServerReceiver(Listener listener){
        // Configure Socekt.IO client

        configureSocketIO();

        mListener = listener;
    }

    private void configureSocketIO(){
        mClient = SocketIOClient.getInstance();

        // TODO : check for nullpointerexception possibility here
        mClient.setServerReceiver(this);

        mClient.on("data", mDataListener);
        mClient.on("start song", mSongStartListener);
        mClient.on("pause", mPauseListener);
        mClient.on("add song", mAddSongListener);
        mClient.on("delete song" , mDeleteSongListener);
        mClient.on("end song", mEndSongListener);
        mClient.on("end session", mEndSessionListener);
        mClient.on("seek" , mSeekListener);
        mClient.on("resume" , mResumeListener);
        mClient.on("play" , mPlayListener);
        mClient.on("update song" , mUpdateSongListener);
    }

    /**
     * The listener for play events
     */
    private Emitter.Listener mPlayListener = new Emitter.Listener(){

        @Override
        public void call(Object... args) {
            mListener.play();
        }
    };


    private boolean mFirstDataReceived = false;
    private long mFirstDataReceivedTime;
    private int mDataReceived = 0;
    private int mCount = 0;

    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener mDataListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            mDataReceived++;
            mCount ++;
            if(!mFirstDataReceived){
                mFirstDataReceived = true;
                mFirstDataReceivedTime = System.currentTimeMillis();
            } else if(mCount >= 100){
                long timeSince = System.currentTimeMillis() - mFirstDataReceivedTime;
                double avgTime = timeSince / mDataReceived;
                mCount = 0;

                Log.d(LOG_TAG, "Average time per frame: " + avgTime + "ms, at " + mDataReceived + " frames received.");
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


            mListener.addFrame(frame);
        }

    };

    private Emitter.Listener mPauseListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            mListener.pause();
        }
    };

    private Emitter.Listener mSongStartListener = new Emitter.Listener() {

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
                mListener.startSong(songStartTime, songID);
            } catch (Exception e){
                e.printStackTrace();
            }

        }
    };

    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener mEndSongListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            int songID = (Integer) args[0];
            mListener.endSong(songID);
        }
    };

    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener mEndSessionListener = new Emitter.Listener() {

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
    private Emitter.Listener mSeekListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            // the first arg should be a long with the seek time
            try {
                Long seekTime = (Long) args[0];
                if (seekTime != null) {
                    mListener.seek(seekTime);
                }
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
     *
     * the new song start time - ms
     *
     */
    private Emitter.Listener mResumeListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            try{
                // Get the resumeTime and the new songStartTime
                JSONObject resumeObject = (JSONObject) args[0];

                // update them
                long resumeTime = resumeObject.getLong("resumeTime");
                Long newSongStartTime = resumeObject.getLong("songStartTime");
                mListener.resume(resumeTime , newSongStartTime);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    /**
     * The socket.IO listener for when a song is added.
     */
    private Emitter.Listener mAddSongListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "add song received from server");
            try{

                JSONObject object = (JSONObject) args[0];
                String type = object.getString("type");
                if(type.equals(UnisongSong.TYPE_STRING)){
                    UnisongSong song = new UnisongSong(object);
                    mListener.addSong(song);
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
    private Emitter.Listener mDeleteSongListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(LOG_TAG , "Delete song received");
            try{
                int songID = (Integer) args[0];

                mListener.deleteSong(songID);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private Emitter.Listener mUpdateSongListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                JSONObject object = (JSONObject) args[1];
                mListener.updateSong(object);
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
        mClient.joinSession(sessionID);
    }
}
