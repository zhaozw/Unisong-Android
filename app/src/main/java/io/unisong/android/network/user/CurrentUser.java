package io.unisong.android.network.user;

import android.content.Context;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;

import io.unisong.android.PrefUtils;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.session.UnisongSession;

/**
 * This class stores information about the current user, including friends list
 * and user profile.
 *
 * Implements the Singleton pattern.
 * Created by Ethan on 9/13/2015.
 */
public class CurrentUser {

    private final static String LOG_TAG = CurrentUser.class.getSimpleName();
    private static User sCurrentUser;
    private FriendsList mFriendsList;
    private HttpClient mClient;

    private static Context sContext;

    public static User getInstance(){
        return sCurrentUser;
    }

    /**
     * Provides a Context from which to load user data. This will load the user data and instantiate
     *
     * @param context
     * @param accountType
     */
    public CurrentUser(Context context, String accountType){
        String username = PrefUtils.getFromPrefs(context , PrefUtils.PREFS_LOGIN_USERNAME_KEY , "");

        sContext = context;

        if(accountType.equals("facebook")){
           // If it's a facebook account load the access token.
            sCurrentUser = new User(context , AccessToken.getCurrentAccessToken());
        } else {
            sCurrentUser = new User(context, username);
        }
    }

    public CurrentUser(Context context, User user){
        sCurrentUser = user;
        sContext = context;

        // If this is a facebook user use the Reflections API to save the
        if(user.isFacebookUser()){
            FacebookAccessToken.saveFacebookAccessToken(context);
        }
    }

    public User getUser(){
        return sCurrentUser;
    }

    /**
     * Logs the current user out. Sends a logout request to the
     */
    public static void logOut(){
        Log.d(LOG_TAG , "Logging Out");
        HttpClient client = HttpClient.getInstance();

        sCurrentUser = null;
        UnisongSession session = UnisongSession.getInstance();

        // delete the Unisong session if we have one
        if(session != null){
            session.destroy();
        }

        // Log out of facebook and get rid of access token.
        if(PrefUtils.getFromPrefs(sContext , PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "unisong").equals("facebook")){
            LoginManager.getInstance().logOut();
            FacebookAccessToken.deleteFacebookAccessToken(sContext);
        }

        // Send logout request
        try {
            client.post(NetworkUtilities.HTTP_URL + "/logout", new JSONObject());
        } catch (IOException e){
            // TODO : manually delete cookie.
            e.printStackTrace();
        }

        // Delete session cookie.
        CookieManager manager = client.getCookieManager();

        for(HttpCookie cookie : manager.getCookieStore().getCookies()){

            Log.d(LOG_TAG, cookie.toString());
            if(cookie.getName().equals("connect.sid")){
                try {
                    // TODO : change this code when we switch to unisong.io
                    manager.getCookieStore().remove(new URI(NetworkUtilities.EC2_INSTANCE + "/"), cookie);
                } catch (URISyntaxException e){
                    e.printStackTrace();
                }
            }
        }


        // Delete stored preferences
        PrefUtils.deleteFromPrefs(sContext, PrefUtils.PREFS_LOGIN_USERNAME_KEY);
        PrefUtils.deleteFromPrefs(sContext , PrefUtils.PREFS_LOGIN_PASSWORD_KEY);

        PrefUtils.deleteFromPrefs(sContext, PrefUtils.PREFS_ACCOUNT_TYPE_KEY);

        Log.d(LOG_TAG , sContext.getCacheDir().getAbsolutePath().toString());
        // Delete cached files
        File dir = new File(sContext.getCacheDir().getAbsolutePath() + "/");
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            Log.d(LOG_TAG , "Deleting " + children.length +" cached files.");
            for (int i = 0; i < children.length; i++)
            {
                Log.d(LOG_TAG , "Deleting : " + children[i]);
                new File(dir, children[i]).delete();
            }
        }

        sContext = null;
    }

}
