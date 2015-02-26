package com.ezturner.speakersync.activity.MusicSelect;

/**
 * Created by Ethan on 2/26/2015.
 */
public class Song implements MusicData{

    private long mId;
    private String mName;
    private String mArtist;
    private String mArt;

    //The class for storing song data
    public Song(long songID, String songTitle, String songArtist, String albumart) {
        mId = songID;
        mName = songTitle;
        mArtist = songArtist;
        mArt = albumart;
    }

    public long getId () {return mId;}

    public String getName () {return mName;}

    //The string that will show up below the name
    public String getSubText() {return mArtist;}

    public String getArtist () {return mArtist;}

    public String getArt(){return mArt;};

    public int getType(){return 3;}
}
