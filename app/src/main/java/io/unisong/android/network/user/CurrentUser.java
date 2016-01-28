package io.unisong.android.network.user;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.CookieManager;
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
    @NonNull
    private static User currentUser;
    private FriendsList friendsList;
    private HttpClient client;

    private static Context context;

    // TODO : investigate subclassing User.
    public static User getInstance(){
        return currentUser;
    }

    /**
     * Provides a Context from which to load user data. This will load the user data and instantiate
     * the current user object
     * @param context
     * @param accountType
     */
    public CurrentUser(Context context, String accountType){
        if(currentUser != null)
            return;

        String username = PrefUtils.getFromPrefs(context , PrefUtils.PREFS_LOGIN_USERNAME_KEY , "");
        String password = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_LOGIN_PASSWORD_KEY , "");

        CurrentUser.context = context;

        if(accountType.equals("facebook")){
            Log.d(LOG_TAG , "Creating Facebook user through saved access token.");
           // If it's a facebook account load the access token.
            currentUser = new User(AccessToken.getCurrentAccessToken());
        } else {
            Log.d(LOG_TAG , "Creating Unisong user through saved username and pass.");
            currentUser = new User(username, password);
        }

        Log.d(LOG_TAG , "Current User : " + currentUser.toString());

        if(currentUser == null)
            Log.d(LOG_TAG , "Current user is null!");

        friendsList = FriendsList.getInstance();

        if(friendsList == null)
            friendsList = new FriendsList(context);
    }

    public CurrentUser(Context context, User user){
        if(currentUser != null)
            return;

        currentUser = user;
        CurrentUser.context = context;

        // If this is a facebook user use the Reflections API to save the
        if(user.isFacebookUser()){
            FacebookAccessToken.saveFacebookAccessToken(context);
        }

        friendsList = FriendsList.getInstance();

        if(friendsList == null)
            friendsList = new FriendsList(context);
    }

    public User getUser(){
        return currentUser;
    }

    /**
     * Logs the current user out. Sends a logout request to the
     */
    public static void logOut(){
        Log.d(LOG_TAG , "Logging Out");
        HttpClient client = HttpClient.getInstance();

        UnisongSession session = currentUser.getSession();
        currentUser = null;

        // delete the Unisong session if we have one
        if(session != null){
            session.destroy();
        }

        UnisongSession.setCurrentSession(null);

        // Log out of facebook and get rid of access token.
        if(PrefUtils.getFromPrefs(context, PrefUtils.PREFS_ACCOUNT_TYPE_KEY, "unisong").equals("facebook")){
            LoginManager.getInstance().logOut();
            FacebookAccessToken.deleteFacebookAccessToken(context);
        }

        // Send logout request
        try {
            client.syncPost(NetworkUtilities.HTTP_URL + "/logout", new JSONObject());
        } catch (IOException e){
            e.printStackTrace();
        }

        // Delete session cookie.
        CookieManager manager = client.getCookieManager();

        for(HttpCookie cookie : manager.getCookieStore().getCookies()){

            Log.d(LOG_TAG, cookie.toString());
            if(cookie.getName().equals("connect.sid")){
                try {
                    manager.getCookieStore().remove(new URI(NetworkUtilities.EC2_INSTANCE + "/"), cookie);
                } catch (URISyntaxException e){
                    e.printStackTrace();
                }
            }
        }


        // Delete stored preferences
        PrefUtils.deleteFromPrefs(context, PrefUtils.PREFS_LOGIN_USERNAME_KEY);
        PrefUtils.deleteFromPrefs(context, PrefUtils.PREFS_LOGIN_PASSWORD_KEY);

        PrefUtils.deleteFromPrefs(context, PrefUtils.PREFS_ACCOUNT_TYPE_KEY);

        FriendsList list = FriendsList.getInstance();

        if(list != null)
            list.destroy();

        Log.d(LOG_TAG, context.getCacheDir().getAbsolutePath().toString());
        // Delete cached files
        File dir = new File(context.getCacheDir().getAbsolutePath() + "/");
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

        context = null;
        System.gc();
    }


    public static void leaveSession(){
        // TODO : see if we need to do anything else? I think these are all the components that need to be dissasembled
        // but I could be wrong.
        UnisongSession session = UnisongSession.getCurrentSession();

        session.leave();

        UnisongSession.setCurrentSession(null);
        currentUser.setSession(null);
    }

}
