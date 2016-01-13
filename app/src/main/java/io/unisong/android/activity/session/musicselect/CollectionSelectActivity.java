package io.unisong.android.activity.session.musicselect;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import java.util.List;

import io.unisong.android.R;

/**
 * Created by Ethan on 10/14/2015.
 */
public class CollectionSelectActivity extends AppCompatActivity {

    private final static String LOG_TAG = CollectionSelectActivity.class.getSimpleName();
    private Toolbar mToolbar;
    private MusicDataManager mDataManager;
    private RecyclerView mMusicDataRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private MusicAdapter mAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_select);

        Intent intent = getIntent();
        // TODO : get album ID from intent
        String tag = intent.getStringExtra("tag");

        mToolbar = (Toolbar) findViewById(R.id.music_bar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setHomeButtonEnabled(true);

        mDataManager = MusicDataManager.getInstance();
        mMusicDataRecyclerView = (RecyclerView) findViewById(R.id.music_recycler_view);

        // use a linear mLayout manager
        mLayoutManager = new LinearLayoutManager(this);
        mMusicDataRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new MusicAdapter(this);


        String[] parts = tag.split(":");

        int type = Integer.parseInt(parts[0]);

        long ID = Long.parseLong(parts[1]);

        List<MusicData> toDisplay = null;
        MusicData data = null;

        switch(type){
            case MusicData.ALBUM:
                data = mDataManager.getAlbumByID(ID);
                break;

            case MusicData.ARTIST:
                data = mDataManager.getArtistByID(ID);
                break;

            case MusicData.GENRE:
                data = mDataManager.getGenreByID(ID);
                break;

            case MusicData.PLAYLIST:
                data = mDataManager.getPlaylistByID(ID);
                break;
        }

        if(data == null){
            Log.d(LOG_TAG, "Data null ):");
            return;
        }

        toDisplay = data.getChildren();


        this.setTitle(data.getPrimaryText());

        mAdapter.setData(toDisplay);
        mMusicDataRecyclerView.setAdapter(mAdapter);
    }

}