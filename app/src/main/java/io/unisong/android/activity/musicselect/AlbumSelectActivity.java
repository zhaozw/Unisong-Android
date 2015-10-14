package io.unisong.android.activity.musicselect;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.List;

import io.unisong.android.R;

/**
 * Created by Ethan on 10/14/2015.
 */
public class AlbumSelectActivity  extends MusicSelectActivity {

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
        long albumID = intent.getLongExtra("albumID" , -1);



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
        if(albumID == -1){
            return;
        }
        List<UISong> mAlbumSongs = mDataManager.getAlbumByID(albumID).getSongs();

        // TODO : see what we need to do to resolve this warning.
        List<MusicData> data = (List<MusicData>)(List<?> )mAlbumSongs;
        mAdapter.setData(data);
        mMusicDataRecyclerView.setAdapter(mAdapter);
    }



}
