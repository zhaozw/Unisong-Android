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

    private Context mContext;
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

        mContext = context;
        sInstance = this;

        if(accountType.equals("facebook")){
           // If it's a facebook account load the access token.

            mCurrentUser = new User(context , username);


        } else {

        }
    }

    public CurrentUser(Context context, User user){
        mCurrentUser = user;
        mFriendsList = FriendsList.getInstance();
        mContext = context;

        // If this is a facebook user use the Reflections API to save the
        if(user.isFacebookUser()){
            FacebookAccessToken.saveFacebookAccessToken(context);
        }


        sInstance = this;
    }


    public User getUser(){
        return mCurrentUser;
    }


}
