package io.unisong.android.activity.session;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

import io.unisong.android.R;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.song.Song;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;

/**
 * Created by Ethan on 10/2/2015.
 */
public class SessionSongsAdapter extends RecyclerView.Adapter<SessionSongsAdapter.ViewHolder> {
    private List<Song> mDataset;

    private final static String LOG_TAG = SessionSongsAdapter.class.getSimpleName();

    private Handler mHandler;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView nameView;
        public TextView artistView;
        public ImageView profileView;

        public ViewHolder(View v) {
            super(v);

            profileView = (ImageView) v.findViewById(R.id.session_song_image);
            nameView = (TextView) v.findViewById(R.id.session_song_name);
            artistView = (TextView) v.findViewById(R.id.session_song_artist);
        }
    }

    /**
     * This will add a song to the end of the SessionSongsAdapter.
     * @param song
     */
    public void add(Song song){
        Log.d(LOG_TAG, "Song Added");
        mDataset.add(song);
        notifyItemInserted(mDataset.size());
    }

    /**
     * This will add a song at the specified position to the Dataset.
     * @param position
     * @param song
     */
    public void add(int position, Song song) {
        mDataset.add(position, song);
        notifyItemInserted(position);
    }

    public void remove(Song song) {
        int position = mDataset.indexOf(song);
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SessionSongsAdapter(List<Song> myDataset) {
        mDataset = myDataset;
        // Give the dataset to the session so that it can be updated manually
        CurrentUser.getInstance().getSession().setSongAdapter(this);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SessionSongsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
        mHandler = new Handler();
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.session_song_row, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        Song song = mDataset.get(position);
        Log.d(LOG_TAG , song.getImageURL());
        Picasso.with(holder.profileView.getContext()).load(new File(song.getImageURL())).into((holder.profileView));
        holder.nameView.setText(mDataset.get(position).getName());
        holder.artistView.setText(mDataset.get(position).getArtist());

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
