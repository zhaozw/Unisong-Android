package com.ezturner.speakersync.network;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * A network session between a variety of devices.
 * This class will contain all the information about the session and those
 * within it, along with the information on current and future songs.
 * Created by Ethan on 8/1/2015.
 */
public class Session {


    private Song mCurrentSong;
    private Queue<Song> mQueuedSongs;
    private String mSesionIdentifier;
    private boolean mIsLocalSession;

    private List<Client> mClients;
    private Master master;

    public Session(){
        mCurrentSong = new Song("Welcome To The Black Parade" , "My Chemical Romance" , 2,
                "audio/mp4a-latm", 0);

        mQueuedSongs = new LinkedList<>();
    }

    public void addClient(Client client){
        for (Client comp : mClients){
            if(comp.equals(client)) return;
        }
    }

    public int getCurrentSongID(){
        return mCurrentSong.getID();
    }

}
