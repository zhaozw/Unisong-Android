package com.ezturner.speakersync.network.slave;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ethan on 4/23/2015.
 */
public class NetworkInputStream extends InputStream {

    private static final String LOG_TAG = "NetworkInputStream";
    private Map<Integer , byte[]> mDatas;
    private int mIndex;
    private int mArrIndex;

    public NetworkInputStream(){
        mDatas = new HashMap<>();
        mIndex = 0;
        mArrIndex = 0;
    }

    public void write(int ID , byte[] data){
        mDatas.put(ID, data);
    }


    @Override
    public int read() throws IOException {
        byte data;
        if(!mDatas.containsKey(mArrIndex)){
            if(mDatas.containsKey(mArrIndex + 1)){
                mArrIndex++;
            }
            return -1;
        }
        data = mDatas.get(mArrIndex)[mIndex];
        mIndex++;

        //If we've reached the end of this array, increase it
        if(mIndex == mDatas.get(mArrIndex).length){
            mDatas.remove(mArrIndex);
            mArrIndex++;
            mIndex = 0;
        }
        return data;
    }

    public boolean isReady(){
        return mDatas.containsKey(mArrIndex);
    }

    public int available(){
        if(mDatas.size() == 0){
            return 0;
        }

        int available = 0;
        for(int i = 0; i < mDatas.size(); i++){
            available += mDatas.get(i).length;
        }

        return available - mIndex;
    }
}
