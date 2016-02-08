package io.unisong.android.activity.friends.contacts;

import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.thedazzler.droidicon.IconicFontDrawable;

import java.util.List;

import io.unisong.android.R;

/**
 * This adapter adapts the Contacts to a RecyclerList.
 * Created by Ethan on 2/4/2016.
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    private final static String LOG_TAG = ContactsAdapter.class.getSimpleName();

    private List<Contact> dataset;

    private Handler handler;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView nameView;
        public Button addButton;
        public RelativeLayout friendLayout;

        public ViewHolder(View v) {
            super(v);

            nameView = (TextView) v.findViewById(R.id.contact_name);
            addButton = (Button) v.findViewById(R.id.add_friend_contact);
        }
    }

    /**
     * Adds a contact to the list at the end
     * @param contact the contact to add
     */
    public void add(Contact contact){
        add(dataset.size(), contact);
    }

    /**
     * Adds a contact at the specified position, then notifies the display
     * @param position the position to add the contact at
     * @param contact  the contact to be added
     */
    public void add(int position, Contact contact) {
        synchronized (dataset) {
            dataset.add(position, contact);
        }
        notifyItemInserted(position);
    }

    /**
     * Removes a contact from the dataset and then notifies the display
     * @param contact the contact to be removed
     */
    public void remove(Contact contact) {
        int position;
        synchronized (dataset) {
            position = dataset.indexOf(contact);
        }
        remove(position);
    }

    /**
     * Removes the contact at the specified position
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
            Contact model = dataset.remove(fromPosition);
            dataset.add(toPosition, model);
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ContactsAdapter(List<Contact> contacts) {
        dataset = contacts;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ContactsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
        handler = new Handler();
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_row, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        Contact contact = dataset.get(position);
        holder.nameView.setText(contact.getName());
        holder.addButton.setTag(contact);
        IconicFontDrawable iconicFontDrawable = new IconicFontDrawable(holder.addButton.getContext());

        if(contact.userExists()) {
            iconicFontDrawable.setIcon("gmd-add");
        } else {
            iconicFontDrawable.setIcon("gmd-person-add");
        }

        iconicFontDrawable.setIconColor(ContextCompat.getColor(holder.addButton.getContext(), R.color.primaryColor));
        iconicFontDrawable.setIconPadding(16);

        holder.addButton.setBackground(iconicFontDrawable);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }

    public void animateTo(List<Contact> data) {
        applyAndAnimateRemovals(data);
        applyAndAnimateAdditions(data);
        applyAndAnimateMovedItems(data);
    }

    private void applyAndAnimateRemovals(List<Contact> newDataset) {
        for (int i = dataset.size() - 1; i >= 0; i--) {
            final Contact model = dataset.get(i);
            if (!newDataset.contains(model)) {
                remove(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<Contact> newDataset) {
        for (int i = 0, count = newDataset.size(); i < count; i++) {
            final Contact model = newDataset.get(i);
            if (!dataset.contains(model)) {
                add(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<Contact> newDataset) {
        for (int toPosition = newDataset.size() - 1; toPosition >= 0; toPosition--) {
            final Contact model = newDataset.get(toPosition);
            final int fromPosition = dataset.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }


}
