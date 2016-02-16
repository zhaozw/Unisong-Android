package io.unisong.android.activity.session.music_select;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import io.unisong.android.R;
import io.unisong.android.audio.MusicDataManager;
import io.unisong.android.audio.song.LocalSong;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.CurrentUser;

/**
 * Created by Ethan on 10/14/2015.
 */
public class CollectionSelectActivity extends AppCompatActivity {

    private final static String LOG_TAG = CollectionSelectActivity.class.getSimpleName();
    private Toolbar toolbar;
    private MusicDataManager dataManager;
    private RecyclerView musicDataRecyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private MusicAdapter adapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_select);

        Intent intent = getIntent();
        // TODO : get album ID from intent
        String tag = intent.getStringExtra("tag");

        toolbar = (Toolbar) findViewById(R.id.music_bar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setHomeButtonEnabled(true);

        dataManager = MusicDataManager.getInstance();
        musicDataRecyclerView = (RecyclerView) findViewById(R.id.music_recycler_view);

        // use a linear mLayout manager
        layoutManager = new LinearLayoutManager(this);
        musicDataRecyclerView.setLayoutManager(layoutManager);

        adapter = new MusicAdapter(this);


        String[] parts = tag.split(":");

        int type = Integer.parseInt(parts[0]);

        long ID = Long.parseLong(parts[1]);

        List<MusicData> toDisplay = null;
        MusicData data = null;

        switch(type){
            case MusicData.ALBUM:
                data = dataManager.getAlbumByID(ID);
                break;

            case MusicData.ARTIST:
                data = dataManager.getArtistByID(ID);
                break;

            case MusicData.GENRE:
                data = dataManager.getGenreByID(ID);
                break;

            case MusicData.PLAYLIST:
                data = dataManager.getPlaylistByID(ID);
                break;
        }

        if(data == null){
            Log.d(LOG_TAG, "Data null ):");
            return;
        }

        toDisplay = data.getChildren();


        this.setTitle(data.getPrimaryText());

        adapter.setData(toDisplay);
        musicDataRecyclerView.setAdapter(adapter);
    }

    public void onRowClick(View v){
        String tag = (String)v.getTag();
        String[] parts = tag.split(":");


        int type = Integer.parseInt(parts[0]);

        long ID = Long.parseLong(parts[1]);

        switch (type){
            case MusicData.ALBUM:
                Intent albumIntent = new Intent(getApplicationContext() , CollectionSelectActivity.class);
                albumIntent.putExtra("tag" , tag);
                startActivity(albumIntent);
                break;

            case MusicData.GENRE:
                Intent genreIntent = new Intent(getApplicationContext() , CollectionSelectActivity.class);
                genreIntent.putExtra("tag" , tag);
                startActivity(genreIntent);
                break;

            case MusicData.PLAYLIST:
                Intent playlistIntent = new Intent(getApplicationContext() , CollectionSelectActivity.class);
                playlistIntent.putExtra("tag" , tag);
                startActivity(playlistIntent);
                break;

            case MusicData.ARTIST:
                Intent artistIntent = new Intent(getApplicationContext() , CollectionSelectActivity.class);
                artistIntent.putExtra("tag" , tag);
                startActivity(artistIntent);
                break;

            case MusicData.SONG:
                Log.d(LOG_TAG , "Song chosen");
                MusicDataManager manager = MusicDataManager.getInstance();
                UISong uiSong = manager.getSongByID(ID);
                LocalSong song = new LocalSong(uiSong);
                UnisongSession session = CurrentUser.getInstance().getSession();
                if(session != null)
                    session.addSong(song);
                Toast toast = Toast.makeText(getBaseContext() , "Song Added", Toast.LENGTH_LONG);
                toast.show();
                break;
        }
    }

}