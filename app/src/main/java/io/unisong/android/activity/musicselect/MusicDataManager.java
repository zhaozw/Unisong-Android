package io.unisong.android.activity.musicselect;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Handler;
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

    private final static String LOG_TAG = MusicDataManager.class.getSimpleName();

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

    //The list of albums on the device
    private List<MusicData> mGenres;

    private Context mContext;
    private Handler mHandler;

    public MusicDataManager(Context applicationContext){
        mContext = applicationContext;
        mHandler = new Handler();
        mHandler.post(mGetMusicInfoRunnable);
    }


    private Runnable mGetMusicInfoRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                getMusicInfo();
            } catch (Exception e){
                e.printStackTrace();
                Log.d(LOG_TAG , "Fetching music data failed!");
            }
        }
    };
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
        mGenres = new ArrayList<>();

        //Get all the artist info
        Uri externalArtistUri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        //Uri internalArtistUri = MediaStore.Audio.Artists.INTERNAL_CONTENT_URI;
        Cursor artistCursor = musicResolver.query(externalArtistUri, null, null, null, null);
        //Cursor internalArtistCursor = musicResolver.query(internalArtistUri , null, null, null, null);

        //Cursor[] artistArr = new Cursor[]{externalArtistCursor , internalArtistCursor};
        //MergeCursor artistCursor = new MergeCursor(artistArr);

        if(artistCursor.moveToFirst()){
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

                artistNameMap.put(artistId, artistName);

                mArtists.add(new UIArtist(artistId, artistName));
            }
            while (artistCursor.moveToNext());
        }
        artistCursor.close();


        Uri externalAlbumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        //Uri internalAlbumUri = MediaStore.Audio.Albums.INTERNAL_CONTENT_URI;
        Cursor albumCursor = musicResolver.query(externalAlbumUri, null, null, null, null);
       // Cursor internalAlbumCursor = musicResolver.query(internalAlbumUri , null, null, null, null);

        //Cursor[] albumArr = new Cursor[]{externalAlbumCursor , internalAlbumCursor};
        //MergeCursor albumCursor = new MergeCursor(albumArr);

        if(albumCursor.moveToFirst()){
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
                String artistName = albumCursor.getString(artistColumn);
                UIArtist artist = getArtistByName(artistName);
                mAlbums.add(new UIAlbum(albumId, albumName , albumArt , artist));
            }
            while (albumCursor.moveToNext());
        }
        albumCursor.close();


        // Merge the internal and external cursors.
        Uri externalMusicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //Uri internalMusicUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(externalMusicUri, null, null, null, null);
        //Cursor internalMusicCursor = musicResolver.query(internalMusicUri , null, null, null, null);

        //Cursor[] musicArr = new Cursor[]{externalMusicCursor , internalMusicCursor};
        //MergeCursor musicCursor = new MergeCursor(musicArr);


        if(musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);

            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);

            int albumIdColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM_ID);


            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);

                //Get the ID of the artist, then get the name
                long artistID = musicCursor.getLong(artistColumn);
                UIArtist artist = getArtistByID(artistID);

                String artistName = artistNameMap.get(artistID);

                long albumID = musicCursor.getLong(albumIdColumn);
                UIAlbum album = getAlbumByID(albumID);

                String albumArt = artMap.get(albumID);


                Log.d(LOG_TAG , "art from map: " + albumArt );
                if(album != null)
                    Log.d(LOG_TAG, "Album Art String : " + album.getImageURL());

                if(!thisTitle.equals("Hangouts message") || !thisTitle.equals("Hangouts video call") || !thisTitle.equals("Facebook Pop")) {

                    UISong song = new UISong(thisId, thisTitle);
                    mSongs.add(song);
                    if(album != null){
                        album.addSong(song);
                    }
                }
            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();




        // Merge the internal and external cursors.
        Uri externalPlaylistUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor playlistCursor = musicResolver.query(externalPlaylistUri, null, null, null, null);


        if(playlistCursor.moveToFirst()){
            //get columns
            int nameColumn = playlistCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.NAME);
            int idColumn = playlistCursor.getColumnIndex
                    (MediaStore.Audio.Playlists._ID);
            int dataColumn = playlistCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.DATA);


            //add songs to list
            do {
                long playlistId = playlistCursor.getLong(idColumn);
                String playlistName = playlistCursor.getString(nameColumn);
                String data = playlistCursor.getString(dataColumn);
                mPlaylists.add(new UIPlaylist(playlistId, playlistName, data));
            }
            while (playlistCursor.moveToNext());
        }
        playlistCursor.close();


        // Merge the internal and external cursors.
        Uri externalGenreUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
        //Uri internalGenreUri = MediaStore.Audio.Genres.INTERNAL_CONTENT_URI;
        Cursor genreCursor = musicResolver.query(externalGenreUri, null, null, null, null);
        //Cursor internalGenreCursor = musicResolver.query(internalGenreUri , null, null, null, null);

        //Cursor[] genreArr = new Cursor[]{externalGenreCursor , internalGenreCursor};
        //MergeCursor genreCursor = new MergeCursor(genreArr);

        if(genreCursor.moveToFirst()){
            //get columns
            int nameColumn = genreCursor.getColumnIndex
                    (MediaStore.Audio.Genres.NAME);
            int idColumn = genreCursor.getColumnIndex
                    (MediaStore.Audio.Genres._ID);



            //add songs to list
            do {
                long genreId = genreCursor.getLong(idColumn);
                String genreName = genreCursor.getString(nameColumn);
                mGenres.add(new UIGenre(genreId, genreName));
            }
            while (genreCursor.moveToNext());
        }
        genreCursor.close();
    }

    public UIArtist getArtistByName(String artist){
        for(MusicData data : mArtists){
            if(data.getPrimaryText().equals(artist)){
                return (UIArtist)data;
            }
        }
        return null;
    }

    public UIArtist getArtistByID(long artistID){
        return (UIArtist) getDataByID(mArtists , artistID);
    }

    public UIAlbum getAlbumByID(long albumID){
        return (UIAlbum) getDataByID(mAlbums , albumID);
    }

    public UISong getSongByID(long songID){
        return (UISong) getDataByID(mSongs , songID);
    }

    public UIGenre getGenreByID(long genreID){
        return (UIGenre) getDataByID(mPlaylists , genreID);
    }

    public UIPlaylist getPlaylistByID(long playlistID){
        return (UIPlaylist) getDataByID(mPlaylists , playlistID);
    }

    private MusicData getDataByID(List<MusicData> datas, long ID){
        for(MusicData data : datas){
            if(data.getID() == ID){
                return data;
            }
        }
        return null;
    }

    public List<MusicData> getData(int pos){
        switch(pos){
            case 0:
                return mPlaylists;
            case 1:
                return mArtists;
            case 2:
                return mAlbums;
            case 3:
                return mSongs;
            // TODO : load genres as well.
            case 4:
                return mGenres;
        }
        return mSongs;
    }

    public void destroy(){
        mContext = null;

    }
}
