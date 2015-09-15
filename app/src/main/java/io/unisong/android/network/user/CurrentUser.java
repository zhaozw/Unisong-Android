package io.unisong.android.network.user;

import android.content.Context;

import io.unisong.android.PrefUtils;

/**
 * This class stores information about the current user, including friends list
 * and user profile.
 *
 * Implements the Singleton pattern.
 * Created by Ethan on 9/13/2015.
 */
public class CurrentUser {

    private User mCurrentUser;
    private FriendsList mFriendsList;

    private static CurrentUser sInstance;

    public static CurrentUser getInstance(){
        return sInstance;
    }

    public CurrentUser(Context context){
        String username = PrefUtils.getFromPrefs(context , PrefUtils.PREFS_LOGIN_USERNAME_KEY , "");
        mCurrentUser = new User(username);
        mFriendsList = new FriendsList();
        sInstance = this;
    }

    public CurrentUser(User user){
        mCurrentUser = user;
        mFriendsList = new FriendsList();
        sInstance = this;
    }


}
