package io.unisong.android.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import io.socket.emitter.Emitter;
import io.unisong.android.R;
import io.unisong.android.network.http.HttpClient;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.SocketIOClient;

import java.io.IOException;


/**
 * Created by Ethan on 9/5/2015.
 */
public class SocketIOTestActivity extends ActionBarActivity {


    private final static String LOG_TAG = SocketIOTestActivity.class.getSimpleName();

    private EditText mSessionID;
    private HttpClient mClient;


    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socketio_test);

        Log.d(LOG_TAG, "Activity Loaded");

        mSessionID = (EditText) findViewById(R.id.session_id);


        mClient = HttpClient.getInstance();
        mClient.login("anoaz" , "pass");


        testThread().start();

    }

    private Thread testThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this){
                    try {
                        this.wait(1050);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }

                    SocketIOClient client = new SocketIOClient();

                }
            }
        });
    }

    public void resume(View view){
        JSONObject obj = new JSONObject();

        try {
            obj.put("resumeTime", 291321902);
        } catch (JSONException e){
            e.printStackTrace();
        }

//        mSocket.emit("resume", obj);
    }

    public void pause(View view){
        JSONObject obj = new JSONObject();

//        mSocket.emit("pause", obj);
    }

    public void joinSession(View view){
        Log.d(LOG_TAG, "Attempting to join session " + mSessionID.getText().toString());
//        mSocket.emit("join session", mSessionID.getText().toString());
    }

    public void createSession(View view){
        getCreateSessionThread().start();
    }

    private Thread getCreateSessionThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                final Response response;
                try {
                    // wtf is this
                    response = mClient.post(NetworkUtilities.HTTP_URL + "/session/", new JSONObject());
                } catch (IOException e){
                    e.printStackTrace();
                    return;
                }

                Log.d(LOG_TAG, response.toString());
                final String id;
                try {
                     id = response.body().string();
                } catch (IOException e){
                    e.printStackTrace();
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                            mSessionID.setText(id);
                    }
                });
            }
        });
    }

    public void createData(View view){
        JSONObject obj = new JSONObject();

        try {
            obj.put("Data", "Data!");
        } catch (JSONException e){
            e.printStackTrace();
        }

//        mSocket.emit("create session", obj);
    }


    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            for(int i = 0; i < args.length; i++) {
                Log.d(LOG_TAG, args[i].toString());
            }
        }
    };

}