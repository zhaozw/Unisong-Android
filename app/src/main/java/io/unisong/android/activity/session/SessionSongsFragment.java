package io.unisong.android.activity.session;


import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

import com.marshalchen.ultimaterecyclerview.UltimateRecyclerView;
import com.marshalchen.ultimaterecyclerview.animators.BaseItemAnimator;
import com.marshalchen.ultimaterecyclerview.animators.FadeInAnimator;
import com.marshalchen.ultimaterecyclerview.itemTouchHelper.SimpleItemTouchHelperCallback;

import java.util.ArrayList;
import java.util.List;

import io.unisong.android.R;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.song.Song;
import io.unisong.android.network.user.CurrentUser;

/**
 * The fragment containing information about the SessionSongs. Displays them to the user.
 * Created by Ethan on 9/26/2015.
 */
public class SessionSongsFragment extends Fragment {

    private List<Song> mSongs;
    private UnisongSession mSession;
    private UltimateRecyclerView mUltimateRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private SessionSongsAdapter mAdapter;


    private ItemTouchHelper mItemTouchHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =inflater.inflate(R.layout.fragment_session_songs, container, false);

        mSession = CurrentUser.getInstance().getSession();
        mUltimateRecyclerView = (UltimateRecyclerView) view.findViewById(R.id.session_songs_recyclerview);

        // use a linear mLayout manager
        mLayoutManager = new LinearLayoutManager(getContext());

        List<Song> songs = new ArrayList<>();
        for(Song song : mSession.getSongQueue().getQueue()){
            songs.add(song);
        }

        mAdapter = new SessionSongsAdapter(songs, mSession.getSongQueue());

        mUltimateRecyclerView.setHasFixedSize(false);

        //mRecyclerView.addItemDecoration(new SimpleListDividerDecorator(ContextCompat.getDrawable(getContext(), R.drawable.list_divider_h), true));

        mUltimateRecyclerView.setLayoutManager(mLayoutManager);
        mUltimateRecyclerView.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mUltimateRecyclerView.mRecyclerView);
        mAdapter.setOnDragStartListener(new SessionSongsAdapter.OnStartDragListener() {
            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                mItemTouchHelper.startDrag(viewHolder);
            }
        });

        return view;
    }

}
