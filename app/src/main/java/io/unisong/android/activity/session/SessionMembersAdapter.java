package io.unisong.android.activity.session;

import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.thedazzler.droidicon.IconicFontDrawable;

import java.util.ArrayList;
import java.util.List;

import io.unisong.android.R;
import io.unisong.android.network.session.SessionMembers;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.user.User;

/**
 * The adapter that adapts a session members
 * Created by ezturner on 9/27/2015.
 */
public class SessionMembersAdapter extends RecyclerView.Adapter<SessionMembersAdapter.ViewHolder>{
    private final static String LOG_TAG = SessionMembersAdapter.class.getSimpleName();
    private List<User> mDataset;

    public static final int ADD = 1823139;
    public static final int REMOVE = 142789;
    private IncomingHandler mHandler;
    private SessionMembers mMembers;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView nameView;
        public TextView usernameView;
        public ImageView profileView;
        public Button kickButton;

        public ViewHolder(View v) {
            super(v);

            profileView = (ImageView) v.findViewById(R.id.friend_image);
            nameView = (TextView) v.findViewById(R.id.friend_first_line);
            usernameView = (TextView) v.findViewById(R.id.friend_second_line);
            kickButton = (Button) v.findViewById(R.id.kick_friend_button);


            UnisongSession session = UnisongSession.getCurrentSession();

            // If we are the session master, then assign a drawable to the kickButton.
            if(session.isMaster()) {
                IconicFontDrawable iconicFontDrawable = new IconicFontDrawable(v.getContext());
                iconicFontDrawable.setIcon("gmd-close");
                iconicFontDrawable.setIconColor(ContextCompat.getColor(v.getContext(), R.color.secondaryText));
                iconicFontDrawable.setIconPadding(24);

                kickButton.setBackground(iconicFontDrawable);
            } else {
                // If not, then hide it
                kickButton.setVisibility(View.GONE);
            }
        }
    }

    public void add(int position, User user) {
        mDataset.add(position, user);
        notifyItemInserted(position);
    }

    public void remove(User user) {
        int position = mDataset.indexOf(user);
        remove(position);
    }

    public void remove(int position){
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SessionMembersAdapter(SessionMembers members) {

        mHandler = new IncomingHandler(this);
        mDataset = new ArrayList<>();

        for(User user : members.getList()){
            mDataset.add(user);
        }

        mMembers = members;
        mMembers.registerHandler(mHandler);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SessionMembersAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.session_member_row, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        User user = mDataset.get(position);
        holder.kickButton.setTag(user.getUUID());
        Picasso.with(holder.profileView.getContext()).load(user.getProfileURL()).into((holder.profileView));
        holder.nameView.setText(mDataset.get(position).getName());
        String userNameText = "@" + mDataset.get(position).getUsername();
        holder.usernameView.setText(userNameText);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }


    public static class IncomingHandler extends Handler{

        private SessionMembersAdapter mAdapter;
        public IncomingHandler(SessionMembersAdapter adapter){
            super();
            mAdapter = adapter;
        }


        @Override
        public void handleMessage(Message message) {

            switch (message.what){
                case ADD:
                    User user;
                    try{
                        user = (User) message.obj;
                    } catch (ClassCastException e){
                        Log.d(LOG_TAG, "ClassCatchException thrown in handleMessage()!");
                        return;
                    }
                    mAdapter.add(message.arg1 , user);
                    break;
                case REMOVE:
                    mAdapter.remove(message.arg1);
                    break;
            }
        }
    }
}