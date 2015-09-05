package io.unisong.android;

import android.app.Application;

import io.unisong.android.network.user.User;

/**
 * Created by Ethan on 2/14/2015.
 */
public class MyApplication extends Application {

    private static boolean activityVisible;

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    private static User sUser;

    public static User getThisUser(){
        return sUser;
    }

    public static void setThisUser(User user){
        sUser = user;
    }


}
