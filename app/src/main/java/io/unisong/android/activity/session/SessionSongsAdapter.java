package io.unisong.android.activity.session;

import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder;
import com.squareup.picasso.Picasso;
import com.thedazzler.droidicon.IconicFontDrawable;

import java.io.File;
import java.util.List;

import io.unisong.android.R;
import io.unisong.android.activity.DrawableUtils;
import io.unisong.android.activity.ViewUtils;
import io.unisong.android.network.session.SongQueue;
import io.unisong.android.network.session.UnisongSession;
import io.unisong.android.network.song.Song;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;

/**
 * Created by Ethan on 10/2/2015.
 */
public class SessionSongsAdapter extends RecyclerView.Adapter<SessionSongsAdapter.ViewHolder>
                                implements DraggableItemAdapter<SessionSongsAdapter.ViewHolder>{
    private List<Song> mDataset;

    private final static String LOG_TAG = SessionSongsAdapter.class.getSimpleName();

    private Handler mHandler;
    private SongQueue mSongQueue;

    // NOTE: Make accessible with short name
    private interface Draggable extends DraggableItemConstants {
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends AbstractDraggableItemViewHolder {
        // each data item is just a string in this case
        public TextView nameView;
        public TextView artistView;
        public ImageView profileView;
        public RelativeLayout mRelativeLayout;
        public Button mRemoveButton;

        public ViewHolder(View v) {
            super(v);

            mRelativeLayout = (RelativeLayout) v.findViewById(R.id.session_song_relative_layout);
            profileView = (ImageView) v.findViewById(R.id.session_song_image);
            nameView = (TextView) v.findViewById(R.id.session_song_name);
            artistView = (TextView) v.findViewById(R.id.session_song_artist);
            mRemoveButton = (Button) v.findViewById(R.id.session_song_remove);


            IconicFontDrawable iconicFontDrawable = new IconicFontDrawable(v.getContext());
            iconicFontDrawable.setIcon("gmd-close");
            iconicFontDrawable.setIconColor(ContextCompat.getColor(v.getContext(), R.color.secondaryText));
            iconicFontDrawable.setIconPadding(24);

            mRemoveButton.setBackground(iconicFontDrawable);
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
        Log.d(LOG_TAG, "Song Added at " + position);
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

        Log.d(LOG_TAG , "songID: " + song.getID());
        holder.mRelativeLayout.setTag(song.getID() + "");
        holder.mRemoveButton.setTag(song.getID() + "");
        Log.d(LOG_TAG , "mRemoveButton Tag: " + holder.mRemoveButton.getTag());
        if(song.getImageURL() != null)
            Picasso.with(holder.profileView.getContext()).load(new File(song.getImageURL())).into((holder.profileView));
        holder.nameView.setText(mDataset.get(position).getName());
        holder.artistView.setText(mDataset.get(position).getArtist());

        // set background resource (target view ID: container)
        final int dragState = holder.getDragStateFlags();

        if (((dragState & Draggable.STATE_FLAG_IS_UPDATED) != 0)) {
            int bgResId;

            if ((dragState & Draggable.STATE_FLAG_IS_ACTIVE) != 0) {
//                bgResId = R.drawable.bg_item_dragging_active_state;

                // need to clear drawable state here to get correct appearance of the dragging item.
//                DrawableUtils.clearState(holder.mContainer.getForeground());
            } else if ((dragState & Draggable.STATE_FLAG_DRAGGING) != 0) {
//                bgResId = R.drawable.bg_item_dragging_state;
            } else {
//                bgResId = R.drawable.bg_item_normal_state;
            }

        }
    }



    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public boolean onCheckCanStartDrag(ViewHolder holder, int position, int x, int y) {
        if(!UnisongSession.getCurrentSession().isMaster())
            return false;

        // x, y --- relative from the itemView's top-left
        final View containerView = holder.mRelativeLayout;
        final View dragHandleView = holder.mRelativeLayout;

        final int offsetX = containerView.getLeft() + (int) (ViewCompat.getTranslationX(containerView) + 0.5f);
        final int offsetY = containerView.getTop() + (int) (ViewCompat.getTranslationY(containerView) + 0.5f);

        return ViewUtils.hitTest(dragHandleView, x - offsetX, y - offsetY);
    }

    @Override
    public ItemDraggableRange onGetItemDraggableRange(ViewHolder holder, int position) {
        return null;
    }

    @Override
    public void onMoveItem(int fromPosition, int toPosition) {
        Log.d(LOG_TAG, "onMoveItem(fromPosition = " + fromPosition + ", toPosition = " + toPosition + ")");

        if (fromPosition == toPosition) {
            return;
        }

        mSongQueue.moveItem(fromPosition, toPosition);

        notifyItemMoved(fromPosition, toPosition);
    }
}
