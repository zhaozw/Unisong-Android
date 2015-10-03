package io.unisong.android.activity.musicselect;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ethan on 10/3/2015.
 */
public class MusicDataManager {

    private final static String LOG_TAG= MusicDataManager.class.getSimpleName();

    private static MusicDataManager sInstance;

    public static MusicDataManager getInstance(){
        return sInstance;
    }

    public static void setInstance(MusicDataManager manager){
        sInstance = manager;
    }

    //The list of songs on the device
    private List<MusicData> mSongs;

    //The list of playlists on the device
    private List<MusicData> mPlaylists;

    //The list of artists on the device
    private List<MusicData> mArtists;

    //The list of albums on the device
    private List<MusicData> mAlbums;

    private Context mContext;

    public MusicDataManager(Context applicationContext){
        mContext = applicationContext;
    }

    //retrieve song info
    private void getMusicInfo() {
        //Get the content resolver
        ContentResolver musicResolver = mContext.getContentResolver();
        //TODO: Verify that EXTERNAL_CONTENT_URI gets all music, and that we don't need to use INTERNAL_CONTENT_URI

        //the map for the art resource urls
        Map<Long, String> artMap = new HashMap<>();

        //The map for the artist IDs to their names
        Map<Long, String> artistNameMap = new HashMap<>();
        mArtists = new ArrayList<>();
        mSongs = new ArrayList<>();
        mAlbums = new ArrayList<>();
        mPlaylists = new ArrayList<>();

        //Get all the artist info
        Uri artistUri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        Cursor artistCursor = musicResolver.query(artistUri, null, null, null, null);

        if(artistCursor != null && artistCursor.moveToFirst()){
            //get columns
            int nameColumn = artistCursor.getColumnIndex
                    (MediaStore.Audio.Artists.ARTIST);
            int idColumn = artistCursor.getColumnIndex
                    (MediaStore.Audio.Artists._ID);

            //add songs to list
            do {
                long artistId = artistCursor.getLong(idColumn);
                String artistName = artistCursor.getString(nameColumn);

                Log.d(LOG_TAG, "Artist Name : " + artistName);

                artistNameMap.put(artistId , artistName);

                mArtists.add(new UIArtist(artistId, artistName, "X albums" , "X Tracks"));
            }
            while (artistCursor.moveToNext());
        }

        //Get the album info
        Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        Cursor albumCursor = musicResolver.query(albumUri, null, null, null, null);


        if(albumCursor != null && albumCursor.moveToFirst()){
            //get columns
            int nameColumn = albumCursor.getColumnIndex
                    (MediaStore.Audio.Albums.ALBUM);
            int idColumn = albumCursor.getColumnIndex
                    (MediaStore.Audio.Albums._ID);

            //I think this is the URI to the art?
            int artColumn = albumCursor.getColumnIndex
                    (MediaStore.Audio.Albums.ALBUM_ART);

            int artistColumn = albumCursor.getColumnIndex
                    (MediaStore.Audio.Albums.ARTIST);

            //add songs to list
            do {
                long albumId = albumCursor.getLong(idColumn);
                String albumName = albumCursor.getString(nameColumn);
                String albumArt = albumCursor.getString(artColumn);
                //Put the album art in the map so that the songs can access it
                artMap.put(albumId , albumArt);
                String albumArtist = albumCursor.getString(artistColumn);
                mAlbums.add(new UIAlbum(albumId, albumName , albumArt , albumArtist));
            }
            while (albumCursor.moveToNext());
        }

        //Get all of the music
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);

            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);

            int albumIdColumn = artistColumn=musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM_ID);

            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);

                //Get the ID of the artist, then get the name
                Long thisArtistId = musicCursor.getLong(artistColumn);
                String thisArtist = artistNameMap.get(thisArtistId);

                long albumId = musicCursor.getLong(albumIdColumn);
                Log.d(LOG_TAG , "Album ID : " + albumId);
                String albumArt = artMap.get(albumId);

                Log.d(LOG_TAG , "Album Art String : " + albumArt);

                mSongs.add(new UISong(thisId, thisTitle, thisArtist , albumArt));
            }
            while (musicCursor.moveToNext());
        }


        //Get all the playlist info
        Uri playlistUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor playlistCursor = musicResolver.query(playlistUri, null, null, null, null);

        if(playlistCursor != null && playlistCursor.moveToFirst()){
            //get columns
            int nameColumn = playlistCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.NAME);
            int idColumn = playlistCursor.getColumnIndex
                    (MediaStore.Audio.Playlists._ID);


            //add songs to list
            do {
                long playlistId = playlistCursor.getLong(idColumn);
                String playlistName = playlistCursor.getString(nameColumn);
                mPlaylists.add(new UIPlaylist(playlistId, playlistName));
            }
            while (playlistCursor.moveToNext());
        }
    }

    public void destroy(){
        mContext = null;

    }
}
