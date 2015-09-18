package io.unisong.android;

import android.content.Context;

import com.facebook.AccessToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * Created by ezturner on 9/18/2015.
 */
public class FacebookAccessToken {

    public static void saveFacebookAccessToken(Context context){

        AccessToken token = AccessToken.getCurrentAccessToken();

        Method toJSONObject;
        try {
            toJSONObject = AccessToken.class.getDeclaredMethod("toJSONObject");
        } catch (NoSuchMethodException e){
            e.printStackTrace();
            return;
        }
        JSONObject obj;
        try{
            obj = (JSONObject)toJSONObject.invoke(token);
        } catch (Exception e){
            e.printStackTrace();
            return;
        }

        PrefUtils.saveToPrefs(context, PrefUtils.PREFS_FACEBOOK_ACCESS_TOKEN_KEY , obj.toString());
    }

    public static AccessToken loadFacebookAccessToken(Context context) {
        String accessToken = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_FACEBOOK_ACCESS_TOKEN_KEY, "");

        if (accessToken.equals("")) {
            // If loading failed, refresh token.
            return null;
        } else {
            JSONObject object;
            try {
                object = new JSONObject(accessToken);
            } catch (JSONException e) {
                // TODO : handle this error
                e.printStackTrace();
                return null;
            }

            Method fromJSONObject;
            try {
                fromJSONObject = AccessToken.class.getMethod("createFromJSONObject");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return null;
            }
            try {
                AccessToken token = (AccessToken) fromJSONObject.invoke(null, object);
                AccessToken.setCurrentAccessToken(token);
                return token;
            } catch (Exception e) {
                e.printStackTrace();
                // todo : handle various exceptions differently
                return null;
            }
        }
    }
}
