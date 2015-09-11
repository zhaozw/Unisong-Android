package io.unisong.android.network.client.receiver;

import java.util.ArrayList;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.network.SocketIOClient;

/**
 * Created by ezturner on 6/8/2015.
 */
public class ServerReceiver implements Receiver{


    private SocketIOClient mClient;

    public ServerReceiver(){
        mClient = new SocketIOClient();
        mClient.joinSession(5);
    }

    @Override
    public ArrayList<AudioFrame> getFrames() {
        return null;
    }


}
