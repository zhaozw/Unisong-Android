package io.unisong.android.activity.unisongsession;

import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.thedazzler.droidicon.IconicFontDrawable;

import java.util.List;

import io.unisong.android.R;
import io.unisong.android.network.user.User;

/**
 * The adapter that adapts a FriendsList to the actual view.
 * Created by ezturner on 8/11/2015.
 */
public class SessionFriendsAdapter extends RecyclerView.Adapter<SessionFriendsAdapter.ViewHolder> {
    private List<User> mDataset;

    private Handler mHandler;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mNameView;
        public TextView mUsernameView;
        public ImageView mProfileView;
        public Button mKickButton;

        public ViewHolder(View v) {
            super(v);

            mProfileView = (ImageView) v.findViewById(R.id.friend_image);
            mNameView = (TextView) v.findViewById(R.id.friend_first_line);
            mUsernameView = (TextView) v.findViewById(R.id.friend_second_line);
            mKickButton = (Button) v.findViewById(R.id.kick_friend_button);


            IconicFontDrawable iconicFontDrawable = new IconicFontDrawable(v.getContext());
            iconicFontDrawable.setIcon("gmd-close");
            iconicFontDrawable.setIconColor(ContextCompat.getColor(v.getContext(), R.color.white));
            iconicFontDrawable.setIconPadding(24);

            mKickButton.setBackground(iconicFontDrawable);
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
    public SessionFriendsAdapter(List<User> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SessionFriendsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
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
        holder.mKickButton.setTag(0 , user.getUUID());
        Bitmap profilePicture = user.getProfilePicture();
        if(profilePicture != null) {
            holder.mProfileView.setImageBitmap(profilePicture);
        } else {
            mHandler.postDelayed(new loadPictureRunnable(user, holder.mProfileView) , 50);
        }
        holder.mNameView.setText(mDataset.get(position).getName());
        holder.mUsernameView.setText("@" + mDataset.get(position).getUsername());

    }


    private class loadPictureRunnable implements Runnable{

        private User mUser;
        private ImageView mImageView;

        public loadPictureRunnable(final User user , final ImageView imageView){
            mUser = user;
            mImageView = imageView;
        }

        @Override
        public void run() {
            if(mUser.profileRetrievalFailed()){

            } else if(!mUser.hasProfilePicture()){
                mHandler.postDelayed(this , 50);
            } else {
                mImageView.setImageBitmap(mUser.getProfilePicture());
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

}