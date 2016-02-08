package io.unisong.android;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import io.unisong.android.network.user.User;

/**
 * Created by Ethan on 2/14/2015.
 */
public class Unisong extends Application {

    private static Unisong instance;

    public static RefWatcher getRefWatcher(Context context) {
        Unisong application = (Unisong) context.getApplicationContext();
        return application.refWatcher;
    }

    private RefWatcher refWatcher;

    public static void setInstance(Unisong application){
        instance = application;
    }

    public static Unisong getInstance(){
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        refWatcher = LeakCanary.install(this);
    }


}
