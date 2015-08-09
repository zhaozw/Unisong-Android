package com.ezturner.speakersync;

import android.app.Application;

import com.ezturner.speakersync.network.user.User;

/**
 * Created by Ethan on 2/14/2015.
 */
public class MyApplication extends Application {

    //The phone's phone number
    private static String sPhoneNumber;

    public static void setPhoneNumber(String number){
        sPhoneNumber = number;
    }

    public static String getPhoneNumber(){
        return sPhoneNumber;
    }

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
