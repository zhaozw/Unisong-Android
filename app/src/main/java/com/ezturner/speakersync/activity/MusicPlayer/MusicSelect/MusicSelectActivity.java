package com.ezturner.speakersync.activity.MusicPlayer.MusicSelect;

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
