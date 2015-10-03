package io.unisong.android.network.packets.tcp;

import io.unisong.android.network.CONSTANTS;
import io.unisong.android.network.song.Song;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;

/**
 * A class for sending all of the relevant information on a UISong,
 * including mime type, artist, duration and name
 * Created by ezturner on 8/10/2015.
 */
public class TCPSongInfoPacket {

    private static final String LOG_TAG = TCPSongInfoPacket.class.getSimpleName();

    private Song mSong;

    public TCPSongInfoPacket(InputStream stream){
        receive(stream);
    }


    public static void send(OutputStream stream, Song song) {

        synchronized (stream) {
            try {
                stream.write(CONSTANTS.TCP_SONG_INFO_PACKET);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(stream);
                objectOutputStream.writeObject(song);
                stream.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void receive(InputStream stream){

        synchronized (stream){
            ObjectInputStream objectInputStream;
            try {
                 objectInputStream = new ObjectInputStream(stream);
            } catch (IOException e){
                e.printStackTrace();
                return;
            }

            try{
                mSong = (Song)objectInputStream.readObject();
                //TODO: figure out how to handle all of these errors
            } catch (OptionalDataException e){
                e.printStackTrace();
            } catch (ClassNotFoundException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }



        }

    }

    public Song getSong(){
        return mSong;
    }
}
