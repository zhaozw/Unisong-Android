package io.unisong.android.activity.session;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.unisong.android.R;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.song.Song;
import io.unisong.android.network.user.CurrentUser;

/**
 * Created by Ethan on 9/26/2015.
 */
public class SessionSongsFragment extends Fragment {

    private List<Song> mSongs;
    private UnisongSession mSession;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private SessionSongsAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =inflater.inflate(R.layout.fragment_session_songs, container, false);

        mSession = CurrentUser.getInstance().getSession();
        mRecyclerView = (RecyclerView) view.findViewById(R.id.session_songs_recyclerview);

        // use this setting to improve performance if you know that changes
        // in content do not change the mLayout size of the RecyclerView

        // use a linear mLayout manager
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        List<Song> songs = new ArrayList<>();
        for(Song song : mSession.getSongQueue().getQueue()){
            songs.add(song);
        }
        mAdapter = new SessionSongsAdapter(songs);
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }
}
