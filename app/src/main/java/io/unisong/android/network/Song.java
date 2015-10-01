package io.unisong.android.network;

/**
 * This is all of the network information on a certain song.
 * Created by Ethan on 8/4/2015.
 */
public class Song {

    private static final byte NAME = 0;
    private static final byte ARTIST = 1;
    private static final byte CHANNELS = 2;
    private static final byte MIME = 3;
    private static final byte DURATION = 4;

    private String mName;
    private String mArtist;
    private int mChannels;
    private String mMime;
    //Duration is in milliseconds
    private long mDuration;
    private String mPath;

    //The # of the song
    private int mSongID;

    //TODO: add the picture for the song in here.

    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     * @param name
     * @param artist
     * @param channels
     * @param mime
     * @param duration
     */
    public Song(String name , String artist, int channels , String mime, long duration){
        mName = name;
        mArtist = artist;
        mChannels = channels;
        mMime = mime;
        mDuration = duration;
    }

    /**
     * This constructor creates a song from the UI/the host, as we will require the path for sourcing requirements.
     */
    public Song(String name , String artist, int channels , String mime, long duration, String path){
        this(name, artist , channels, mime, duration);
        mPath = path;
    }
    /**
     * This is a recursive method to decode a byte array into a Song object
     *
     * @param data the raw network data for this client object
     * @param index the index at which we are currently operating
     */
    //TODO: test and make sure this recursion works.
    private void decode(byte[] data , int index){
        //Increment the index for the switch statement
        index++;
        switch (data[index - 1]){
            case NAME:
                mName = NetworkUtilities.decodeString(data, index);
                break;
            case ARTIST:
                mArtist = NetworkUtilities.decodeString(data, index);
                break;
            case MIME:
                mMime = NetworkUtilities.decodeString(data, index);
                break;
            case CHANNELS:
                mChannels = NetworkUtilities.decodeInt(data, index);
                break;
            case DURATION:
                mDuration = NetworkUtilities.decodeLong(data , index);
                break;

        }

        // If the index is either greater than the data length or at the end of the data, then
        // return and don't recursively call
        if(index< data.length -1 ){
            decode(data , index);
        }
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

}
