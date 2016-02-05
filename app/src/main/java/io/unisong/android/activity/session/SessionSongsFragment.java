package io.unisong.android.activity.session;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.marshalchen.ultimaterecyclerview.UltimateRecyclerView;
import com.marshalchen.ultimaterecyclerview.itemTouchHelper.SimpleItemTouchHelperCallback;

import java.util.ArrayList;
import java.util.List;

import io.unisong.android.R;
import io.unisong.android.audio.song.Song;
import io.unisong.android.network.session.SongQueue;
import io.unisong.android.network.session.UnisongSession;

/**
 * The fragment containing information about the SessionSongs. Displays them to the user.
 * Created by Ethan on 9/26/2015.
 */
public class SessionSongsFragment extends Fragment {

    private final static String LOG_TAG = SessionSongsFragment.class.getSimpleName();

    private UnisongSession session;
    private UltimateRecyclerView ultimateRecyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private FloatingActionButton FAB;
    private SessionSongsAdapter adapter;


    private ItemTouchHelper mItemTouchHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =inflater.inflate(R.layout.fragment_session_songs, container, false);

        try {
            session = UnisongSession.getCurrentSession();
            ultimateRecyclerView = (UltimateRecyclerView) view.findViewById(R.id.session_songs_recyclerview);

            if (session == null)
                Log.d(LOG_TAG, "Session is null!");

            // use a linear mLayout manager
            layoutManager = new LinearLayoutManager(getContext());

            List<Song> songs = new ArrayList<>();
            SongQueue queue = session.getSongQueue();

            if (queue == null)
                Log.d(LOG_TAG, "queue is null!");

            for (Song song : queue.getQueue()) {
                songs.add(song);
            }

            FAB = (FloatingActionButton)  view.findViewById(R.id.unisong_fab);


            if(!session.isMaster())
                FAB.setVisibility(View.GONE);

            adapter = new SessionSongsAdapter(songs, session.getSongQueue());

            ultimateRecyclerView.setHasFixedSize(false);

            //mRecyclerView.addItemDecoration(new SimpleListDividerDecorator(ContextCompat.getDrawable(getContext(), R.drawable.list_divider_h), true));

            ultimateRecyclerView.setLayoutManager(layoutManager);
            ultimateRecyclerView.setAdapter(adapter);

            ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
            mItemTouchHelper = new ItemTouchHelper(callback);
            mItemTouchHelper.attachToRecyclerView(ultimateRecyclerView.mRecyclerView);
            adapter.setOnDragStartListener(new SessionSongsAdapter.OnStartDragListener() {
                @Override
                public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                    mItemTouchHelper.startDrag(viewHolder);
                }
            });
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        return view;
    }

}
