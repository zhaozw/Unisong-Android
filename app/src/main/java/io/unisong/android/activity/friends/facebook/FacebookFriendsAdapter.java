package io.unisong.android.activity.friends.facebook;

import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.thedazzler.droidicon.IconicFontDrawable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.unisong.android.R;
import io.unisong.android.network.user.User;

/**
 * Created by Ethan on 2/15/2016.
 */
public class FacebookFriendsAdapter extends RecyclerView.Adapter<FacebookFriendsAdapter.ViewHolder> {

    private final static String LOG_TAG = FacebookFriendsAdapter.class.getSimpleName();

    private List<User> dataset;
    private List<User> originalDataset;

    private Handler handler;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView nameView;
        public TextView usernameView;
        public Button addButton;
        public RelativeLayout friendLayout;

        public ViewHolder(View v) {
            super(v);

            nameView = (TextView) v.findViewById(R.id.user_name);
            usernameView = (TextView) v.findViewById(R.id.user_username);
            addButton = (Button) v.findViewById(R.id.add_friend_button);
        }
    }

    /**
     * Adds a User to the list at the end
     * @param User the User to add
     */
    public void add(User User){
        add(dataset.size(), User);
    }

    /**
     * Adds a User at the specified position, then notifies the display
     * @param position the position to add the User at
     * @param User  the User to be added
     */
    public void add(int position, User User) {
        synchronized (dataset) {
            dataset.add(position, User);
        }
        notifyItemInserted(position);
    }

    /**
     * Removes a User from the dataset and then notifies the display
     * @param User the User to be removed
     */
    public void remove(User User) {
        int position;
        synchronized (dataset) {
            position = dataset.indexOf(User);
        }
        remove(position);
    }

    /**
     * Removes the User at the specified position
     * @param position - the position from which to remove an element
     */
    public void remove(int position){
        synchronized (dataset){
            dataset.remove(position);
        }
        notifyItemRemoved(position);
    }

    /**
     * Moves an item from fromPosition to toPosition
     * @param fromPosition the position to move from
     * @param toPosition   the position to move to
     */
    public void moveItem(int fromPosition, int toPosition) {
        synchronized (dataset) {
            User model = dataset.remove(fromPosition);
            dataset.add(toPosition, model);
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public FacebookFriendsAdapter() {
        dataset = new ArrayList<>();
        originalDataset = new ArrayList<>();

        // TODO : implement after figuring out how to parse friends

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/{friend-list-id}",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Log.d(LOG_TAG, response.getJSONObject().toString());
                    }
                }
        ).executeAsync();

        
    }

    // Create new views (invoked by the layout manager)
    @Override
    public FacebookFriendsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
        handler = new Handler();
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.facebook_row, parent, false);
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
        holder.nameView.setText(user.getName());
        holder.usernameView.setText(user.getUsername());
        holder.addButton.setTag(user);

        IconicFontDrawable iconicFontDrawable = new IconicFontDrawable(holder.addButton.getContext());
        iconicFontDrawable.setIcon("gmd-add");
        iconicFontDrawable.setIconColor(ContextCompat.getColor(holder.addButton.getContext(), R.color.primaryColor));
        iconicFontDrawable.setIconPadding(16);

        holder.addButton.setBackground(iconicFontDrawable);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }

    public void animateTo(List<User> data) {
        applyAndAnimateRemovals(data);
        applyAndAnimateAdditions(data);
        applyAndAnimateMovedItems(data);
    }

    private void applyAndAnimateRemovals(List<User> newDataset) {
        for (int i = dataset.size() - 1; i >= 0; i--) {
            final User model = dataset.get(i);
            if (!newDataset.contains(model)) {
                remove(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<User> newDataset) {
        for (int i = 0, count = newDataset.size(); i < count; i++) {
            final User model = newDataset.get(i);
            if (!dataset.contains(model)) {
                add(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<User> newDataset) {
        for (int toPosition = newDataset.size() - 1; toPosition >= 0; toPosition--) {
            final User model = newDataset.get(toPosition);
            final int fromPosition = dataset.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    public List<User> getOriginalUsers(){
        return originalDataset;
    }
}