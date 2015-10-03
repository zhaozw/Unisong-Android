package io.unisong.android.network.song;

/**
 * This is all of the network information on a certain song.
 * Created by Ethan on 8/4/2015.
 */
public class Song {
    
    private String mName;
    private String mArtist;
    private int mChannels;
    private String mMime;
    //Duration is in milliseconds
    private long mDuration;
    private int mType;

    private String mImageURL;

    //The # of the song
    private int mSongID;

    // TODO : put the AudioFileReader/SongDecoder stuff in here
    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     * @param name
     * @param artist
     * @param channels
     * @param mime
     * @param duration
     */
    public Song(String name , String artist, int channels , String mime, long duration, String imageURL){
        mName = name;
        mArtist = artist;
        mChannels = channels;
        mMime = mime;
        mDuration = duration;
        mImageURL = imageURL;
    }

    /**
     * This constructor creates a song from the UI/the host, as we will require the path for sourcing requirements.
     */
    public Song(String name , String artist, int channels , String mime, long duration, String imageURL , String path){
        this(name, artist , channels, mime, duration, imageURL);
    }

    public int getID(){
        return mSongID;
    }

    public String getName(){
        return mName;
    }

    public String getMime(){
        return mMime;
    }

    public String getArtist(){return mArtist;}

    public int getChannels(){
        return mChannels;
    }

    public long getDuration(){
        return mDuration;
    }

    //TODO: implement this
    public byte[] getBytes(){
        return new byte[1];
    }

    public String getPath(){
        return mPath;
    }

    public String getImageURL(){
        return mImageURL;
    }
}
