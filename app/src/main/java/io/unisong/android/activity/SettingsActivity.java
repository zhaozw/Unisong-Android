package io.unisong.android.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import io.unisong.android.R;

/**
 * Created by ezturner on 9/18/2015.
 */
public class SettingsActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_settings);
    }
}
