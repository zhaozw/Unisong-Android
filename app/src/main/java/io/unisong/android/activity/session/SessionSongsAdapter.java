package io.unisong.android.activity.session;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.marshalchen.ultimaterecyclerview.UltimateDifferentViewTypeAdapter;
import com.marshalchen.ultimaterecyclerview.UltimateRecyclerviewViewHolder;
import com.marshalchen.ultimaterecyclerview.UltimateViewAdapter;
import com.marshalchen.ultimaterecyclerview.dragsortadapter.DragSortAdapter;
import com.squareup.picasso.Picasso;
import com.thedazzler.droidicon.IconicFontDrawable;

import java.io.File;
import java.util.List;

import io.unisong.android.R;
import io.unisong.android.audio.AudioStatePublisher;
import io.unisong.android.network.session.SongQueue;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.song.Song;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;

/**
 * Created by Ethan on 10/2/2015.
 */
public class SessionSongsAdapter extends UltimateViewAdapter<SessionSongsAdapter.ViewHolder> {
    private List<Song> mDataset;

    private final static String LOG_TAG = SessionSongsAdapter.class.getSimpleName();

    private SongQueue mSongQueue;

    // NOTE: Make accessible with short name

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends UltimateRecyclerviewViewHolder{
        // each data item is just a string in this case
        public TextView nameView;
        public TextView artistView;
        public ImageView profileView;
        public RelativeLayout mRelativeLayout;
//        public Button mRemoveButton;

        public ViewHolder(View v, boolean isItem) {
            super(v);

            // ???
            if(!isItem)
                return;

            mRelativeLayout = (RelativeLayout) v.findViewById(R.id.session_song_relative_layout);
            profileView = (ImageView) v.findViewById(R.id.session_song_image);
            nameView = (TextView) v.findViewById(R.id.session_song_name);
            artistView = (TextView) v.findViewById(R.id.session_song_artist);
//            mRemoveButton = (Button) v.findViewById(R.id.session_song_remove);
//
//            IconicFontDrawable iconicFontDrawable = new IconicFontDrawable(v.getContext());
//            iconicFontDrawable.setIcon("gmd-close");
//            iconicFontDrawable.setIconColor(ContextCompat.getColor(v.getContext(), R.color.secondaryText));
//            iconicFontDrawable.setIconPadding(24);
//
//            mRemoveButton.setBackground(iconicFontDrawable);
        }
    }

    /**
     * This will add a song to the end of the SessionSongsAdapter.
     * @param song
     */
    public void add(Song song){
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
    public SessionSongsAdapter(List<Song> myDataset, SongQueue queue) {
        mDataset = myDataset;
        mSongQueue = queue;
        // Give the dataset to the session so that it can be updated manually
        try {
            User user = CurrentUser.getInstance();
            UnisongSession session = user.getSession();
            session.setSongAdapter(this);
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        setHasStableIds(true);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SessionSongsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.session_song_row, parent, false);

        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v, true);
        return vh;
    }

    @Override
    public ViewHolder getViewHolder(View view) {
        return new ViewHolder(view, false);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return null;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Song song = mDataset.get(position);

        holder.mRelativeLayout.setTag(song.getID() + "");
//        holder.mRemoveButton.setTag(song.getID() + "");
        if(song.getImageURL() != null && !song.getImageURL().contains("http://")) {
            Picasso.with(holder.profileView.getContext()).load(new File(song.getImageURL())).into((holder.profileView));
        } else {

        }
        holder.nameView.setText(mDataset.get(position).getName());
        holder.artistView.setText(mDataset.get(position).getArtist());

    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        return null;
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public int getAdapterItemCount() {
        return mDataset.size();
    }

    @Override
    public long generateHeaderId(int position) {
        return 0;
    }

    public void setOnDragStartListener(OnStartDragListener dragStartListener) {
        mDragStartListener = dragStartListener;
    }

    @Override
    public void onItemDismiss(int position) {
        mSongQueue.remove(position);
    }

    @Override
    public void onItemMove(int startPos, int endPosition){
        // If we're playing and startPos or endPos == 0, then let's not interrupt the current song
        if(AudioStatePublisher.getInstance().getState() == AudioStatePublisher.PLAYING) {
            if (startPos == 0)
                return;

            if(endPosition == 0)
                endPosition = 1;
        }

        super.onItemMove(startPos, endPosition);
        mSongQueue.move(startPos, endPosition);
    }

}
