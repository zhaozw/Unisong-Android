package io.unisong.android.activity.friends.contacts;

import android.graphics.Canvas;
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
 * This adapter adapts the Contacts to a RecylcerList. No mutability, so it's pretty simple.
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

    public void add(int position, Contact contact) {
        dataset.add(position, contact);
        notifyItemInserted(position);
    }

    public void remove(Contact contact) {
        int position = dataset.indexOf(contact);
        dataset.remove(position);
        notifyItemRemoved(position);
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

}
