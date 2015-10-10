package io.unisong.android.network.client.receiver;

import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.socket.emitter.Emitter;
import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.audio.AudioTrackManager;
import io.unisong.android.network.TimeManager;
import io.unisong.android.network.client.Listener;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.SocketIOClient;

/**
 * Created by ezturner on 6/8/2015.
 */
public class ServerReceiver implements Receiver{

    private final static String LOG_TAG = ServerReceiver.class.getSimpleName();

    private SocketIOClient mClient;

    private Map<Integer, AudioFrame> mFrames;
    private Listener mListener;

    private AudioTrackManager mAudioTrackManager;

    public ServerReceiver(Listener listener){
        // Configure Socekt.IO client

        configureSocketIO();
        // Initialize frames.
        mFrames = new HashMap<>();

        mListener = listener;
    }

    private void configureSocketIO(){
        mClient = new SocketIOClient();

        mClient.setServerReceiver(this);

        mClient.on("data", mDataListener);
        mClient.on("start song", mSongStartListener);
        mClient.on("pause", mPauseListener);
        mClient.on("end song", mEndSongListener);
        mClient.on("end session", mEndSessionListener);
        mClient.on("seek" , mSeekListener);
        mClient.on("add song" , mAddSongListener);

    }

    @Override
    public Map<Integer , AudioFrame> getFrames() {
        return mFrames;
    }



    private boolean mFirstDataReceived = false;
    private long mFirstDataReceivedTime;
    private int mDataReceived = 0;

    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener mDataListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            mDataReceived++;
            if(!mFirstDataReceived){
                mFirstDataReceived = true;
                mFirstDataReceivedTime = System.currentTimeMillis();
            } else if(mDataReceived % 500 == 0 || (mDataReceived < 50 && mDataReceived % 5 == 0)){
                long timeSince = System.currentTimeMillis() - mFirstDataReceivedTime;
                double avgTime = timeSince / mDataReceived;

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



        }
    };

    private Emitter.Listener mSongStartListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {

            Log.d(LOG_TAG , "Server UISong Start received.");
            JSONObject object = (JSONObject) args[0];
            long songStartTime;
            int songID;
            int channels;
            try {
                songStartTime = object.getLong("songStartTime");
                songID = object.getInt("songID");
                channels = object.getInt("channels");
            } catch (JSONException e){
                e.printStackTrace();
                return;
            }

            mListener.startSong(songStartTime , channels ,songID);
        }
    };

    private Emitter.Listener mEmitterListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            JSONObject object = (JSONObject) args[0];
        }
    };

    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener mEndSongListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {

        }
    };

    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener mEndSessionListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {

        }
    };


    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener mSeekListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {

        }
    };


    /**
     * The listener for when we get data.
     *
     */
    private Emitter.Listener mAddSongListener = new Emitter.Listener() {

        @Override
        public void call(Object... args) {

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
