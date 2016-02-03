package io.unisong.android.audio.song;

import org.json.JSONObject;

import io.unisong.android.audio.AudioFrame;
import io.unisong.android.audio.AudioObserver;
import io.unisong.android.audio.decoder.Decoder;

/**
 * This is all of the network information on a certain song.
 * Created by Ethan on 8/4/2015.
 */
public abstract class Song implements AudioObserver {

    private static final String LOG_TAG = Song.class.getSimpleName();
    protected String name, artist , imageURL;
    protected boolean started;

    protected Decoder decoder;

    //The # of the song
    protected int songID;

    /**
     * This is the constructor for a song created from a network source. We do not need the path
     * since we will be taking it in over wifi.
     * @param name
     * @param artist
     * @param imageURL
     */
    public Song(String name , String artist, int ID , String imageURL){
        this.name = name;
        this.artist = artist;
        this.imageURL = imageURL;
        songID = ID;
        started = false;
    }

    public Song(String name , String artist, String imageURL){
        this.name = name;
        this.artist = artist;
        this.imageURL = imageURL;
        started = false;
    }

    public int getID(){
        return songID;
    }

    public String getName(){
        return name;
    }

    public String getArtist(){return artist;}

    public abstract String getImageURL();

    /**
     * Returns the encoded frame with the specified ID
     * @param ID
     * @return
     */
    public abstract AudioFrame getAACFrame(int ID);

    /**
     * Returns the raw PCM frame with a given ID
     * @param ID
     * @return
     */
    public abstract AudioFrame getPCMFrame(int ID);

    public abstract boolean hasAACFrame(int ID);

    public abstract boolean hasPCMFrame(int ID);

    public abstract void start();

    public abstract void seek(long seekTime);

    public boolean hasPCMFrameAtTime(long time){
        return decoder.hasFrameAtTime(time);
    }

    public int getFrameIDForTime(long time){
        return decoder.getFrameIDAtTime(time);
    }

    public abstract SongFormat getFormat();

    public abstract void addFrame(AudioFrame frame);

    public abstract long getDuration();

    public abstract JSONObject toJSON();

    public abstract void update(JSONObject songJSON);

    public boolean started(){
        return started;
    }
}