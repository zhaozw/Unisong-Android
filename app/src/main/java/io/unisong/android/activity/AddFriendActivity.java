package io.unisong.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import io.unisong.android.R;

/**
 * Created by Ethan on 9/22/2015.
 */
public class AddFriendActivity extends AppCompatActivity{


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);
    }

    public void addFromContacts(View view){
        Intent intent = new Intent(getApplicationContext(), AddFriendFromContactsActivity.class);
        startActivity(intent);
    }

    public void addByUsername(View view){
        Intent intent = new Intent(getApplicationContext(), AddFriendByUsernameActivity.class);
        startActivity(intent);
    }
}
