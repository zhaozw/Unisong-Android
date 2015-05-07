package com.ezturner.speakersync.activity.musicplayer.MusicSelect;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import com.ezturner.speakersync.R;

/**
 * Created by Ethan on 2/26/2015.
 */
public class MusicSelectActivity extends Activity{

    private final String LOG_TAG = "MusicSelectActivity";



    private Intent mIntent;

    private MusicAdapter mAlphabeticalAdapter;

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




    }


}
