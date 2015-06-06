package com.ezturner.speakersync.audio.master;

import com.ezturner.speakersync.MediaService;
import com.ezturner.speakersync.activity.MusicPlayer.MusicSelect.Song;

/**
 * Created by Ethan on 6/4/2015.
 */
public class CurrentSongInfo {

    private static CurrentSongInfo sInstance = new CurrentSongInfo(null);
    public static CurrentSongInfo getInstance(){
        return sInstance;
    }

    private Song mSong;
    private int mChannels;

    public CurrentSongInfo(Song song){
        mSong = song;
        sInstance = this;
        mChannels = 2;
    }

    public String getFilePath(){
        return MediaService.TEST_FILE_PATH;
//        return mSong.getData();
    }

    public void setChannels(int channels){
        mChannels = channels;
    }

    public int getChannels(){
        return mChannels;
    }
}
