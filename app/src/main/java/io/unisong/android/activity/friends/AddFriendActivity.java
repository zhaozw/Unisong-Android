package io.unisong.android.activity.friends;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import io.unisong.android.R;

/**
 * Created by Ethan on 9/22/2015.
 */
public class AddFriendActivity extends AppCompatActivity{


    private Toolbar mToolbar;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);


        mToolbar = (Toolbar) findViewById(io.unisong.android.R.id.music_bar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setHomeButtonEnabled(true);
    }

    public void addFromContacts(View view){
        Intent intent = new Intent(getApplicationContext(), AddFriendsFromContactsActivity.class);
        startActivity(intent);
    }

    public void addByUsername(View view){
        Intent intent = new Intent(getApplicationContext(), AddFriendByUsernameActivity.class);
        startActivity(intent);
    }


    /**
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(io.unisong.android.R.menu.music_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if(id == R.id.action_settings){
            Toast.makeText(this, "Hey, you just hit the button! ", Toast.LENGTH_SHORT).show();
            return true;
        } else if(id == R.id.action_add_friend){
            Intent intent = new Intent(getApplicationContext() , AddFriendActivity.class);
            startActivity(intent);
            return true;
        } else if(id == R.id.action_log_out){

            return true;
        }

        return super.onOptionsItemSelected(item);
    }**/
}
