package com.ezturner.speakersync;

import android.app.Application;

/**
 * Created by Ethan on 2/14/2015.
 */
public class MyApplication extends Application {

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    private static boolean activityVisible;
    private static boolean isPlaying;

    public static boolean isPlaying(){
        return isPlaying;
    }

    public static void play(){
        isPlaying = true;
    }

    public static void stop(){
        isPlaying = false;
    }
}
