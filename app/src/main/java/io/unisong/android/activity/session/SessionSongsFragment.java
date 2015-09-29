package io.unisong.android.activity.session;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.unisong.android.R;

/**
 * Created by Ethan on 9/26/2015.
 */
public class SessionSongsFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =inflater.inflate(R.layout.fragment_session_songs, container, false);



        return view;
    }
}
