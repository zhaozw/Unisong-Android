package io.unisong.android.activity.friends.facebook;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.unisong.android.R;
import io.unisong.android.network.user.FriendsList;
import io.unisong.android.network.user.User;
import io.unisong.android.network.user.UserUtils;

/**
 * This activity is used in the registration process to 
 * Created by Ethan on 2/4/2016.
 */
public class AddFriendsFromFacebookActivity extends AppCompatActivity implements android.support.v7.widget.SearchView.OnQueryTextListener {

    private static final String LOG_TAG = AddFriendsFromFacebookActivity.class.getSimpleName();
    private RecyclerView contactsView;
    private FacebookFriendsAdapter fbFriendsAdapter;
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend_from_facebook);

        toolbar = (Toolbar) findViewById(io.unisong.android.R.id.add_friend_bar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setHomeButtonEnabled(true);

        fbFriendsAdapter = new FacebookFriendsAdapter();

        contactsView = (RecyclerView) findViewById(R.id.contacts_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getBaseContext());
        contactsView.setLayoutManager(layoutManager);
        contactsView.setAdapter(fbFriendsAdapter);
        contactsView.setHasFixedSize(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);

        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This is the method called when the add user button is clicked from the
     * contact_row
     * @param view
     */
    public void addFriendFromContactsClick(View view){
        User user = null;
        try {
            user = (User)view.getTag();
        } catch (ClassCastException e){
            e.printStackTrace();
            Log.d(LOG_TAG , "View did not have tag of class Contact!");
            return;
        }
        FriendsList.getInstance().addFriendToList(user, addFriendCallback);
    }

    private Callback addFriendCallback = new Callback() {

        @Override
        public void onFailure(Request request, IOException e) {

        }

        @Override
        public void onResponse(Response response) throws IOException {
            if(response.code() == 200){
                FriendsList list = FriendsList.getInstance();
                list.addFriendToList(UserUtils.getUser(response.body().string()));
                runOnUiThread(() -> {
                    Toast toast = Toast.makeText(getBaseContext() , "Friend Added!" , Toast.LENGTH_LONG);
                    toast.show();
                });
            }
        }
    };

    @Override
    public boolean onQueryTextChange(String query) {
        final List<User> filteredModelList = filter(fbFriendsAdapter.getOriginalUsers(), query);
        fbFriendsAdapter.animateTo(filteredModelList);
        contactsView.scrollToPosition(0);
        return true;
    }

    private List<User> filter(List<User> models, String query) {
        query = query.toLowerCase();

        final List<User> filteredModelList = new ArrayList<>();
        for (User model : models) {
            final String text = model.getName().toLowerCase();
            if (text.contains(query)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }
}
