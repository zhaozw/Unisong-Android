package io.unisong.android.activity.musicplayer;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import io.unisong.android.activity.musicselect.MusicData;
import io.unisong.android.activity.musicselect.UIAlbum;
import io.unisong.android.activity.musicselect.UIArtist;
import io.unisong.android.activity.musicselect.UIPlaylist;
import io.unisong.android.activity.musicselect.UISong;

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
    private List<UISong> mUISongs;

    //The list of playlists on the device
    private List<UIPlaylist> mPlaylists;

    //The list of artists on the device
    private List<UIArtist> mUIArtists;

    //The list of albums on the device
    private List<UIAlbum> mUIAlbums;

    private ContentResolver mContentResolver;

    public MusicDataHandler(ContentResolver contentResolver){

        mContentResolver = contentResolver;

        //Start the array lists of music data
        mUISongs = new ArrayList<UISong>();
        mPlaylists = new ArrayList<UIPlaylist>();
        mUIAlbums = new ArrayList<UIAlbum>();
        mUIArtists = new ArrayList<UIArtist>();

        //get all of the music info
        getMusicInfo();

        mListData = new ArrayList<MusicData>();
        //The for loop for turning our data into a generic List for the AlphabeticalAdapter list, so we can filter out Hangouts stuff
        for(int i = 0; i < mUISongs.size(); i++){
            String name = mUISongs.get(i).getPrimaryText();
            if(!name.equals("Hangouts message") && !name.equals("Hangouts video call")){
                mListData.add(mUISongs.get(i));
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

                mUIArtists.add(new UIArtist(artistId, artistName, albums, tracks));
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
                mUIAlbums.add(new UIAlbum(albumId, albumName , albumArt , albumArtist));
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
                Log.d(LOG_TAG , "UIAlbum ID : " + albumId);
                String albumArt = artMap.get(albumId);

                Log.d(LOG_TAG , "UIAlbum Art String : " + albumArt);

                mUISongs.add(new UISong(thisId, thisTitle, thisArtist , albumArt));
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
                mPlaylists.add(new UIPlaylist(playlistId, playlistName));
            }
            while (playlistCursor.moveToNext());
        }
        playlistCursor.close();
    }
}
