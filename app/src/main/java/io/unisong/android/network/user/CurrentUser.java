package io.unisong.android.network.user;

import android.content.Context;

import io.unisong.android.PrefUtils;
import io.unisong.android.network.http.HttpClient;

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
    private HttpClient mClient;

    private static CurrentUser sInstance;

    public static CurrentUser getInstance(){
        return sInstance;
    }

    /**
     * Provides a Context from which to load user data. This will load the user data and instantiate
     *
     * @param context
     * @param accountType
     */
    public CurrentUser(Context context, String accountType){
        String username = PrefUtils.getFromPrefs(context , PrefUtils.PREFS_LOGIN_USERNAME_KEY , "");
        mCurrentUser = new User(username);
        mFriendsList = FriendsList.getInstance();
        sInstance = this;
    }

    public CurrentUser(User user){
        mCurrentUser = user;
        mFriendsList = FriendsList.getInstance();
        sInstance = this;
    }


    public User getUser(){
        return mCurrentUser;
    }


}
