package io.unisong.android.activity.musicplayer;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import io.unisong.android.activity.musicplayer.musicselect.MusicData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Work on 4/11/2015.
 */
public class MusicDataHandler {

    private final String LOG_TAG = "MusicDataHandler";

    //The strings being displayed in the listView
    private List<MusicData> mListData;

    //The list of songs on the device
    private List<io.unisong.android.activity.musicplayer.musicselect.LocalSong> mLocalSongs;

    //The list of playlists on the device
    private List<io.unisong.android.activity.musicplayer.musicselect.LocalPlaylist> mPlaylists;

    //The list of artists on the device
    private List<io.unisong.android.activity.musicplayer.musicselect.LocalArtist> mLocalArtists;

    //The list of albums on the device
    private List<io.unisong.android.activity.musicplayer.musicselect.LocalAlbum> mLocalAlbums;

    private ContentResolver mContentResolver;

    public MusicDataHandler(ContentResolver contentResolver){

        mContentResolver = contentResolver;

        //Start the array lists of music data
        mLocalSongs = new ArrayList<io.unisong.android.activity.musicplayer.musicselect.LocalSong>();
        mPlaylists = new ArrayList<io.unisong.android.activity.musicplayer.musicselect.LocalPlaylist>();
        mLocalAlbums = new ArrayList<io.unisong.android.activity.musicplayer.musicselect.LocalAlbum>();
        mLocalArtists = new ArrayList<io.unisong.android.activity.musicplayer.musicselect.LocalArtist>();

        //get all of the music info
        getMusicInfo();

        mListData = new ArrayList<MusicData>();
        //The for loop for turning our data into a generic List for the AlphabeticalAdapter list, so we can filter out Hangouts stuff
        for(int i = 0; i < mLocalSongs.size(); i++){
            String name = mLocalSongs.get(i).getPrimaryText();
            if(!name.equals("Hangouts message") && !name.equals("Hangouts video call")){
                mListData.add(mLocalSongs.get(i));
            }
        }
    }
    //retrieve song info
    private void getMusicInfo(){
        //Get the content resolver

        //TODO: Verify that EXTERNAL_CONTENT_URI gets all music, and that we don't need to use INTERNAL_CONTENT_URI

        //the map for the art resource urls
        Map<Long, String> artMap = new HashMap<Long , String>();

        //The map for the artist IDs to their names
        Map<Long, String> artistNameMap = new HashMap<Long , String>();

        //Get all the artist info
        Uri artistUri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        Cursor artistCursor = mContentResolver.query(artistUri, null, null, null, null);

        if(artistCursor != null && artistCursor.moveToFirst()){
            //get columns
            int nameColumn = artistCursor.getColumnIndex
                    (MediaStore.Audio.Artists.ARTIST);
            int idColumn = artistCursor.getColumnIndex
                    (MediaStore.Audio.Artists._ID);
            int albumsColumn = artistCursor.getColumnIndex(
                    MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
            int tracksColumn = artistCursor.getColumnIndex(
                    MediaStore.Audio.Artists.NUMBER_OF_TRACKS);

            //add songs to list
            do {
                long artistId = artistCursor.getLong(idColumn);
                String artistName = artistCursor.getString(nameColumn);
                String albums = artistCursor.getString(albumsColumn);
                String tracks = artistCursor.getString(tracksColumn);
                Log.d(LOG_TAG , "Albums is: " + albums + " , and tracks: " + tracks);

                artistNameMap.put(artistId , artistName);

                mLocalArtists.add(new io.unisong.android.activity.musicplayer.musicselect.LocalArtist(artistId, artistName, albums, tracks));
            }
            while (artistCursor.moveToNext());
        }

        artistCursor.close();

        //Get the album info
        Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        Cursor albumCursor = mContentResolver.query(albumUri, null, null, null, null);


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
                mLocalAlbums.add(new io.unisong.android.activity.musicplayer.musicselect.LocalAlbum(albumId, albumName , albumArt , albumArtist));
            }
            while (albumCursor.moveToNext());
        }
        albumCursor.close();

        //Get all of the music
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = mContentResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);

            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);

            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);

            int albumIdColumn =musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM_ID);

            int dataColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DATA);

            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);

                //Get the ID of the artist, then get the name
                Long thisArtistId = musicCursor.getLong(artistColumn);
                String thisArtist = artistNameMap.get(thisArtistId);

                long albumId = musicCursor.getLong(albumIdColumn);

                String data = musicCursor.getString(dataColumn);
                Log.d(LOG_TAG , "LocalAlbum ID : " + albumId);
                String albumArt = artMap.get(albumId);

                Log.d(LOG_TAG , "LocalAlbum Art String : " + albumArt);

                mLocalSongs.add(new io.unisong.android.activity.musicplayer.musicselect.LocalSong(thisId, thisTitle, thisArtist , albumArt, data));
            }
            while (musicCursor.moveToNext());
        }

        musicCursor.close();

        //Get all the playlist info
        Uri playlistUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor playlistCursor = mContentResolver.query(playlistUri, null, null, null, null);

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
                mPlaylists.add(new io.unisong.android.activity.musicplayer.musicselect.LocalPlaylist(playlistId, playlistName));
            }
            while (playlistCursor.moveToNext());
        }
        playlistCursor.close();
    }
}
