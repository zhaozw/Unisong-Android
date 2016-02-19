package io.unisong.android.activity.friends;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import io.unisong.android.R;
import io.unisong.android.network.user.User;

/**
 * The adapter that adapts a FriendsList to the actual view.
 * Created by ezturner on 8/11/2015.
 */
public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private final static String LOG_TAG = FriendsAdapter.class.getSimpleName();

    private List<User> dataset;

    private SwipeRefreshLayout refreshLayout;
    private Handler handler;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView nameView;
        public TextView usernameView;
        public ImageView profileView;
        public RelativeLayout friendLayout;

        public ViewHolder(View v) {
            super(v);

            friendLayout = (RelativeLayout) v.findViewById(R.id.friend_row_layout);
            profileView = (ImageView) v.findViewById(R.id.friend_image);
            nameView = (TextView) v.findViewById(R.id.friend_first_line);
            usernameView = (TextView) v.findViewById(R.id.friend_second_line);
        }
    }

    public void add(int position, User user) {
        dataset.add(position, user);
        notifyItemInserted(position);
    }

    public void remove(User user) {
        int position = dataset.indexOf(user);
        dataset.remove(position);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public FriendsAdapter(List<User> myDataset) {
        dataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public FriendsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        handler = new Handler();
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_row, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        User user = dataset.get(position);

        holder.friendLayout.setTag(user.getUUID());
        holder.profileView.setTag(user.getUUID());
        holder.usernameView.setTag(user.getUUID());
        holder.nameView.setTag(user.getUUID());
        if(user.getName() != null && user.getUsername() != null) {
            Picasso.with(holder.profileView.getContext()).load(user.getProfileURL()).into((holder.profileView));
            holder.nameView.setText(dataset.get(position).getName());
            holder.usernameView.setText("@" + dataset.get(position).getUsername());
        } else {
            new LoadUserProfile(holder, user).execute();
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }

    private class LoadUserProfile extends AsyncTask<Void , Void, Void> {


        private ViewHolder holder;
        private User user;

        public LoadUserProfile(ViewHolder holder, User user){
            this.holder = holder;
            this.user = user;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            synchronized (this){
                try {
                    this.wait(100);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            while(user.getName() == null || user.getUsername() == null){

                synchronized (this){
                    try {
                        this.wait(10);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nullObj) {
            Picasso.with(holder.profileView.getContext())
                    .load(user.getProfileURL())
                    .into((ImageView) holder.profileView.findViewById(R.id.friend_image));

            Log.d(LOG_TAG, "Current User done loading profile picture, assigning to ImageView");

            Log.d(LOG_TAG, "user: " + user.toString());

            String usersName = user.getName();

            holder.nameView.setText(usersName);


            TextView username = (TextView) holder.profileView.findViewById(R.id.current_user_username);

            if(username != null)
                username.setText("@" + user.getUsername());
        }
    }


}