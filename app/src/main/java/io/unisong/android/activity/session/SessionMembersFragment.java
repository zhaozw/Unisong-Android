package io.unisong.android.activity.session;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.unisong.android.R;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.CurrentUser;


/**
 * Created by Ethan on 9/26/2015.
 */
public class SessionMembersFragment extends Fragment{


    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private SessionMembersAdapter mAdapter;
    private UnisongSession mSession;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_session_friends, container, false);

        mSession = CurrentUser.getInstance().getSession();
        mRecyclerView = (RecyclerView) view.findViewById(R.id.members_recyclerview);

        // use this setting to improve performance if you know that changes
        // in content do not change the mLayout size of the RecyclerView

        // use a linear mLayout manager
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);


        // specify an adapter (see also next example)
        mAdapter = new SessionMembersAdapter(mSession.getMembers());
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

}
