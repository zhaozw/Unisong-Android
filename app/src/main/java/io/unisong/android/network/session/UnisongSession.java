package io.unisong.android.network.session;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import io.unisong.android.network.Client;
import io.unisong.android.network.Master;
import io.unisong.android.network.Song;

/**
 * A network session between a variety of devices.
 * This class will contain all the information about the session and those
 * within it, along with the information on current and future songs.
 *
 * Utilizes the Singleton design pattern.
 * Created by Ethan on 8/1/2015.
 */
public class UnisongSession {


    private static UnisongSession sInstance;

    public static UnisongSession getInstance(){
        return sInstance;
    }

    private Song mCurrentSong;
    private String mSesionIdentifier;
    private boolean mIsLocalSession;
    private SongQueue mSongQueue;

    private List<Client> mClients;
    private Master master;

    public UnisongSession(){
        Song firstSong = new Song("Welcome To The Black Parade" , "My Chemical Romance" , 2,
                "audio/mp4a-latm", 0);

        mSongQueue = new SongQueue();
        mSongQueue.addSong(firstSong);

        sInstance = this;
    }

    public void addClient(Client client){
        for (Client comp : mClients){
            if(comp.equals(client)) return;
        }
    }

    public int getCurrentSongID(){
        return mCurrentSong.getID();
    }

    public Song getCurrentSong(){
        return mCurrentSong;
    }

    public void startSong(int songID){
        mCurrentSong = mSongQueue.getSong(songID);
    }
}
