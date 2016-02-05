package io.unisong.android.activity.friends.contacts;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.List;

import io.unisong.android.R;
import io.unisong.android.network.user.FriendsList;
import io.unisong.android.network.user.UserUtils;

/**
 * This class has a list of the user's contacts, loaded from the Android
 * ContactsContract provider. Through this, the user can see which of their friends
 * Are on  Unisong, and invite any that are not.
 * Created by Ethan on 2/4/2016.
 */
public class AddFriendsFromContactsActivity extends AppCompatActivity implements android.widget.SearchView.OnQueryTextListener {

    private static final String LOG_TAG = AddFriendsFromContactsActivity.class.getSimpleName();
    private RecyclerView contactsView;
    private ContactsLoader contactsLoader;
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend_from_contacts);
        contactsLoader = new ContactsLoader(getBaseContext());

        toolbar = (Toolbar) findViewById(io.unisong.android.R.id.add_friend_bar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setHomeButtonEnabled(true);

        ContactsLoader contactsLoader = new ContactsLoader(getBaseContext());

        List<Contact> contacts = contactsLoader.getContacts();

        Log.d(LOG_TAG, "Size of contacts is : " + contacts.size());

        ContactsAdapter adapter = new ContactsAdapter(contacts);

        contactsView = (RecyclerView) findViewById(R.id.contacts_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getBaseContext());
        contactsView.setLayoutManager(layoutManager);
        contactsView.setAdapter(adapter);
        contactsView.setHasFixedSize(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);

        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This is the method called when the add user button is clicked from the
     * contact_row
     * @param view
     */
    public void addFriendFromContactsClick(View view){
        Contact contact = null;
        try {
            contact = (Contact)view.getTag();
        } catch (ClassCastException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "View did not have tag of class Contact!");
            return;
        }
        if(contact.userExists()) {
            FriendsList.getInstance().addFriend(UserUtils.getUser(contact.getUserUUID()), addFriendCallback);
        } else {
            // TODO : implement text invite
        }
    }

    private Callback addFriendCallback = new Callback() {

        @Override
        public void onFailure(Request request, IOException e) {

        }

        @Override
        public void onResponse(Response response) throws IOException {
            if(response.code() == 200){
                FriendsList list = FriendsList.getInstance();
                list.addFriend(UserUtils.getUser(response.body().string()));
                runOnUiThread(() -> {
                    Toast toast = Toast.makeText(getBaseContext() , "Friend Added!" , Toast.LENGTH_LONG);
                    toast.show();
                });
            }
        }
    };

    @Override
    public boolean onQueryTextChange(String query) {
        // Here is where we are going to implement our filter logic
        // TODO : keep going with stackoverflow.com/questions/30398247/how-to-filter-a-recyclerview-with-a-searchview
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }
}
