package io.unisong.android.network.user;

import android.content.Context;
import android.util.Log;

import com.facebook.AccessToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

import io.unisong.android.PrefUtils;

/**
 * Created by ezturner on 9/18/2015.
 */
public class FacebookAccessToken {

    private final static String LOG_TAG = FacebookAccessToken.class.getSimpleName();

    public static void saveFacebookAccessToken(Context context){

        AccessToken token = AccessToken.getCurrentAccessToken();

        Method toJSONObject;
        try {
            toJSONObject = AccessToken.class.getDeclaredMethod("toJSONObject");
        } catch (NoSuchMethodException e){
            e.printStackTrace();
            return;
        }

        toJSONObject.setAccessible(true);
        JSONObject obj;
        try{
            obj = (JSONObject)toJSONObject.invoke(token);
        } catch (Exception e){
            e.printStackTrace();
            return;
        }

        PrefUtils.saveToPrefs(context, PrefUtils.PREFS_FACEBOOK_ACCESS_TOKEN_KEY, obj.toString());
    }

    public static void loadFacebookAccessToken(Context context) {
        String accessToken = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_FACEBOOK_ACCESS_TOKEN_KEY, "");

        if (accessToken.equals("")) {
            // If loading failed, refresh token.
            return;
        } else {
            JSONObject object;
            try {
                object = new JSONObject(accessToken);
            } catch (JSONException e) {
                // TODO : handle this error
                e.printStackTrace();
                return;
            }

            Method fromJSONObject;
            try {
                fromJSONObject = AccessToken.class.getDeclaredMethod("createFromJSONObject", JSONObject.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return;
            }
            fromJSONObject.setAccessible(true);

            try {
                AccessToken token = (AccessToken) fromJSONObject.invoke(null, object);
                if(token != null && !token.isExpired()){

                    AccessToken.setCurrentAccessToken(token);
                    Log.d(LOG_TAG, token.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                // todo : handle various exceptions differently
            }
        }
    }

    public static void deleteFacebookAccessToken(Context context){
        PrefUtils.deleteFromPrefs(context , PrefUtils.PREFS_FACEBOOK_ACCESS_TOKEN_KEY);
    }
}
