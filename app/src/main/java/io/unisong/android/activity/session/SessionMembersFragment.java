package io.unisong.android.activity.session;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.unisong.android.R;
import io.unisong.android.network.session.UnisongSession;


/**
 * Created by Ethan on 9/26/2015.
 */
public class SessionMembersFragment extends Fragment{

    private final static String LOG_TAG = SessionMembersFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SessionMembersAdapter adapter;
    private UnisongSession session;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_session_members, container, false);

        try {
            session = UnisongSession.getCurrentSession();

            recyclerView = (RecyclerView) view.findViewById(R.id.members_recyclerview);
            swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);

            swipeRefreshLayout.setOnRefreshListener(() -> {
                UnisongSession session = UnisongSession.getCurrentSession();
                if(session != null) {
                    session.getUpdate();
                    session.setRefreshLayout(getActivity(), swipeRefreshLayout);
                }
            });

            // use this setting to improve performance if you know that changes
            // in content do not change the mLayout size of the RecyclerView

            // use a linear mLayout manager
            layoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(layoutManager);


            // specify an adapter (see also next example)
            adapter = new SessionMembersAdapter(session.getMembers() , false);
            recyclerView.setAdapter(adapter);
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        return view;
    }

}
