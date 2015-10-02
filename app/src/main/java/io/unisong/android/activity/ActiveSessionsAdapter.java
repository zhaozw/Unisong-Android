package io.unisong.android.activity;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import io.unisong.android.R;
import io.unisong.android.network.user.User;

/**
 * This is the adapter that handles the Active Sessions.
 * Created by Ethan on 10/1/2015.
 */
public class ActiveSessionsAdapter extends RecyclerView.Adapter<ActiveSessionsAdapter.ViewHolder>  {

    private List<User> mDataset;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ActiveSessionsAdapter(List<User> myDataset) {
        mDataset = myDataset;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_row, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // TODO : figure out how to indicate active sessions.
        User user = mDataset.get(position);
        Picasso.with(holder.profileView.getContext()).load(user.getProfileURL()).into((holder.profileView));
        holder.nameView.setText(mDataset.get(position).getName());
        holder.usernameView.setText("@" + mDataset.get(position).getUsername());
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }


    public void add(int position, User user) {
        mDataset.add(position, user);
        notifyItemInserted(position);
    }

    public void remove(User user) {
        int position = mDataset.indexOf(user);
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView nameView;
        public TextView usernameView;
        public ImageView profileView;

        public ViewHolder(View v) {
            super(v);

            profileView = (ImageView) v.findViewById(R.id.friend_image);
            nameView = (TextView) v.findViewById(R.id.friend_first_line);
            usernameView = (TextView) v.findViewById(R.id.friend_second_line);
        }
    }

}
