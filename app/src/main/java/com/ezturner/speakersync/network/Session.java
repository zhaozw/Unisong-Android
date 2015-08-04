package com.ezturner.speakersync.network;

import com.ezturner.speakersync.network.master.Client;

import java.util.LinkedList;
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

    public Session(){
        mCurrentSong = new Song("Welcome To The Black Parade" , "My Chemical Romance" , 2,
                "audio/mp4a-latm", 0);

        mQueuedSongs = new LinkedList<>();
    }

    public void addClient(Client client){

    }

    public int getCurrentSongID(){
        return mCurrentSong.getID();
    }

}
