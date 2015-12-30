package io.unisong.android.activity.friends;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import io.unisong.android.R;
import io.unisong.android.network.user.User;

import java.util.List;

/**
 * The adapter that adapts a FriendsList to the actual view.
 * Created by ezturner on 8/11/2015.
 */
public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private final static String LOG_TAG = FriendsAdapter.class.getSimpleName();

    private List<User> mDataset;

    private Handler mHandler;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
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

    public void add(int position, User user) {
        mDataset.add(position, user);
        notifyItemInserted(position);
    }

    public void remove(User user) {
        int position = mDataset.indexOf(user);
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public FriendsAdapter(List<User> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public FriendsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        mHandler = new Handler();
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

        User user = mDataset.get(position);
        if(user.getName() != null && user.getUsername() != null) {
            Picasso.with(holder.profileView.getContext()).load(user.getProfileURL()).into((holder.profileView));
            holder.nameView.setText(mDataset.get(position).getName());
            holder.usernameView.setText("@" + mDataset.get(position).getUsername());
        } else {
            new LoadUserProfile(holder, user).execute();
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    private class LoadUserProfile extends AsyncTask<Void , Void, Void> {


        private ViewHolder mHolder;
        private User mUser;

        public LoadUserProfile(ViewHolder holder, User user){
            mHolder = holder;
            mUser = user;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            while(mUser.getName() == null || mUser.getUsername() == null){

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
            Picasso.with(mHolder.profileView.getContext()).load(mUser.getProfileURL()).into((ImageView) mHolder.profileView.findViewById(R.id.friend_image));

            Log.d(LOG_TAG, "Current User done loading profile picture, assigning to ImageView");
            TextView name = (TextView) mHolder.profileView.findViewById(R.id.current_user_name);
            Log.d(LOG_TAG , "mUser: " + mUser.toString());

            // TODO : investigate null case
            if(name != null)
                Log.d(LOG_TAG , "name : " + name.toString());
            name.setText(mUser.getName());

            TextView username = (TextView) mHolder.profileView.findViewById(R.id.current_user_username);
            username.setText("@" + mUser.getUsername());

        }
    }

}