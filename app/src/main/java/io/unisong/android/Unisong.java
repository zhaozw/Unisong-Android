package io.unisong.android;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

/**
 * This is the Application class used to A. create and instantiate the Crashlytics instance and
 * provide access to the static application context in case it is required.
 *
 * It also allows for MultiDex support.
 * Created by Ethan on 2/14/2015.
 */
public class Unisong extends Application {

    private static Unisong instance;

    public static void setInstance(Unisong application){
        instance = application;
    }

    public static Unisong getInstance(){
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
//        refWatcher = LeakCanary.install(this);

    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


}