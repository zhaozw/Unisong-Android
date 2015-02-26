package com.ezturner.speakersync.activity.MusicSelect;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ListView;

import com.ezturner.speakersync.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ethan on 2/26/2015.
 */
public class MusicSelectActivity extends Activity{

    private final String LOG_TAG = "MusicSelectActivity";

    //The strings being displayed in the listView
    private List<MusicData> mListData;

    //The list of songs on the device
    private List<Song> mSongs;

    //The list of playlists on the device
    private List<Playlist> mPlaylists;

    //The list of artists on the device
    private List<Artist> mArtists;

    //The list of albums on the device
    private List<Album> mAlbums;

    private Intent mIntent;

    private AlphabeticalAdapter mAlphabeticalAdapter;

    private ListView mSongList;

    @Override
    public void onCreate(Bundle savedInstanceState){

        //Create the activity and set the layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_select);

        mSongList = (ListView) findViewById(R.id.music_list);

        mIntent = getIntent();
        boolean hasStarted = mIntent.getBooleanExtra("has-started" , false);

        if(hasStarted) return;

        //Start the array lists of music data
        mSongs = new ArrayList<Song>();
        mPlaylists = new ArrayList<Playlist>();
        mAlbums = new ArrayList<Album>();
        mArtists = new ArrayList<Artist>();

        //get all of the music info
        getMusicInfo();

        mListData = new ArrayList<MusicData>();
        //The for loop for turning our data into a generic List for the AlphabeticalAdapter list, so we can filter out Hangouts stuff
        for(int i = 0; i < mSongs.size(); i++){
            String name = mSongs.get(i).getName();
            if(!name.equals("Hangouts message") && !name.equals("Hangouts video call")) {
                mListData.add(mSongs.get(i));
            }
        }

        mAlphabeticalAdapter = new AlphabeticalAdapter(this , R.layout.music_list_item , mListData);

        mSongList.setAdapter(mAlphabeticalAdapter);
    }


    //retrieve song info
    private void getMusicInfo() {
        //Get the content resolver
        ContentResolver musicResolver = getContentResolver();
        //TODO: Verify that EXTERNAL_CONTENT_URI gets all music, and that we don't need to use INTERNAL_CONTENT_URI

        //the map for the art resource urls
        Map<Long, String> artMap = new HashMap<Long , String>();

        //The map for the artist IDs to their names
        Map<Long, String> artistNameMap = new HashMap<Long , String>();

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

                Log.d(LOG_TAG,"Artist Name : " + artistName);

                artistNameMap.put(artistId , artistName);

                mArtists.add(new Artist(artistId, artistName));
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
                mAlbums.add(new Album(albumId, albumName , albumArt , albumArtist));
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

                mSongs.add(new Song(thisId, thisTitle, thisArtist , albumArt));
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
                mPlaylists.add(new Playlist(playlistId, playlistName));
            }
            while (playlistCursor.moveToNext());
        }





    }


}
