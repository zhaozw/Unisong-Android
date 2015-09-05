package io.unisong.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import io.unisong.android.activity.MusicPlayer.MusicPlayer;

/**
 * Created by Ethan on 2/25/2015.
 */
public class TypeSelector extends Activity {

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(io.unisong.android.R.layout.type_selector);

    }

    public void onClickMaster(View v){
        Intent intent = new Intent(this, MusicPlayer.class);
        startActivity(intent);
    }

    public void onClickSlave(View v){
        Intent intent = new Intent(this, SlaveActivity.class);
        startActivity(intent);
    }
}
