package io.unisong.android.network.user;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.unisong.android.activity.UnisongActivity;
import io.unisong.android.activity.friends.FriendsAdapter;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;

/**
 * The class that holds the Friends List. Implements the Singleton design pattern.
 *
 * Created by Ethan on 8/9/2015.
 */
public class FriendsList implements Serializable{

    private final static String FILE_NAME = "FriendsList";
    private final static String LOG_TAG = FriendsList.class.getSimpleName();

    private static UnisongActivity activityToNotify;
    private static FriendsList instance;
    private FriendsAdapter adapter;

    public static FriendsList getInstance(){
        return instance;
    }

    public static void setActivityToNotify(UnisongActivity activity){
        activityToNotify = activity;
    }

    private HttpClient client;
    private List<User> friends;
    private List<User> incomingRequests;
    private List<User> outgoingRequests;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Users not currently in an Unisong session.
    private List<User> idleUsers;

    // Users currently in an active Unisong session.
    private List<User> activeUsers;

    private Context context;

    private Thread friendsThread;

    // This tells us whether the data has been updated and needs to be written to disk
    private boolean updated;
    private Handler handler;

    /**
     * Instantiates the FriendsList, and loads the relevant data from disk if available
     * and the network if not.
     * Also checks to see if the data on disk is up to date.
     */
    public FriendsList(Context context){
        instance = this;
        updated = false;
        this.context = context;

        // TODO : handle not being logged in and having no data on disk.
        // TODO : implement storage with file system not Prefs.

        // these will be overwritten if read from the disk
        friends = new ArrayList<>();
        incomingRequests = new ArrayList<>();
        outgoingRequests = new ArrayList<>();

        try {
            handler = new Handler();
        } catch (RuntimeException e){
            Looper.prepare();
            handler = new Handler();
        }

        client = HttpClient.getInstance();
        // TODO : get friends from server.
        // TODO : store and only update.
        // TODO : reenable after testing FB integration.


        loadFriends();

        if(activityToNotify != null){
            activityToNotify.setFriendsList(this);
        }
    }

    private void loadFriends(){

        friendsThread = getFriendsThread();
        friendsThread.start();
    }


    private void waitForLogin(){
        // Wait until we're logged in and have a CurrentUser to get info from.
        // TODO : can the httpclient be null here? Are there situations in which we should handle this?
        while(!client.isLoginDone() && !client.isLoggedIn()){
            // TODO : see if we're waiting forever
            synchronized (this){
                try{
                    this.wait(20);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }


    private Thread getFriendsThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {

                waitForLogin();

                loadFriendsFromServer();

                if(activityToNotify != null){
                    activityToNotify.friendsListLoaded();
                }
                handler.post(checkFriendStatusRunnable);

            }
        });
    }

    /**
     * Starts a thread that updates the Friends List through the server.
     */
    public void update(){
        getUpdateThread().start();
    }

    private Thread getUpdateThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                loadFriendsFromServer();
            }
        });
    }

    private void loadFriendsFromServer(){
        Log.d(LOG_TAG, "Sent GET to /user/friends");
        HttpClient client = HttpClient.getInstance();

        while(!client.isLoggedIn()){
            synchronized (this){
                try{
                    this.wait(20);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
        String URL = NetworkUtilities.HTTP_URL + "/user/friends";

        Response response;
        try {
            response = client.syncGet(URL);
        } catch (IOException e){
            e.printStackTrace();
            Log.d(LOG_TAG, "Request Failed");
            return;
        }


        Log.d(LOG_TAG , "FriendsList Received");
        JSONObject body;
        try {
            String bodyString = response.body().string();
            body = new JSONObject(bodyString);

            JSONArray friendsArray = body.getJSONArray("friends");
            Log.d(LOG_TAG , "friends : " + friendsArray.toString());
            JSONArray inReqArray = body.getJSONArray("incomingRequests");
            Log.d(LOG_TAG , "Incoming Requests : " + incomingRequests.toString());
            JSONArray outReqAr = body.getJSONArray("outgoingRequests");
            Log.d(LOG_TAG, "Outgoing Requests : " + outReqAr.toString());

            // Load friends
            for(int i = 0; i < friendsArray.length(); i++){

                JSONObject object = friendsArray.getJSONObject(i);
                User userToAdd = UserUtils.getUser(UUID.fromString(object.getString("userID")));
                boolean isEqual = false;

                // Make sure the user we're loading isn't already in friends
                for(User user : friends){
                    if(user.equals(userToAdd)){
                        isEqual = true;
                        break;
                    }
                }

                // remove any duplicates and then add
                if(!isEqual) {
                    updated = true;
                    removeDuplicateUsers(userToAdd , incomingRequests);
                    removeDuplicateUsers(userToAdd , outgoingRequests);
                    friends.add(userToAdd);
                }
            }

            for(int i = 0; i < inReqArray.length(); i++){

                JSONObject object = inReqArray.getJSONObject(i);
                User userToAdd = new User(UUID.fromString(object.getString("userID")));
                boolean isEqual = false;

                for(User user : incomingRequests){
                    if(user.equals(userToAdd)){
                        isEqual = true;
                        break;
                    }
                }

                // remove any duplicates and then add
                if(!isEqual) {
                    updated = true;
                    removeDuplicateUsers(userToAdd , outgoingRequests);
                    removeDuplicateUsers(userToAdd , friends);
                    incomingRequests.add(userToAdd);
                }
            }

            for(int i = 0; i < outReqAr.length(); i++){

                JSONObject object = outReqAr.getJSONObject(i);
                User userToAdd = new User(UUID.fromString(object.getString("userID")));
                boolean isEqual = false;

                // make sure we dont' already have this user.
                for(User user : outgoingRequests){
                    if(user.equals(userToAdd)){
                        break;
                    }
                }

                // remove any duplicates and then add
                if(!isEqual) {
                    updated = true;
                    removeDuplicateUsers(userToAdd , incomingRequests);
                    removeDuplicateUsers(userToAdd , friends);
                    outgoingRequests.add(userToAdd);
                }
            }

            if(updated){
                // TODO : enable caching.
                //writeToDisk();
            }

            if(swipeRefreshLayout != null)
                swipeRefreshLayout.setRefreshing(false);

            if(adapter != null)
                adapter.notifyDataSetChanged();
        } catch (IOException e){
            // TODO : handle
            e.printStackTrace();
            return;
        } catch (JSONException e){
            int dles = 0;
            e.printStackTrace();
            return;
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * This will check for duplicate users in  all lists and remove any that exist.
     */
    private void removeDuplicateUsers(User user, List<User> userList){
        // See if there are any duplicates

        for(int i = 0; i < userList.size(); i++){
            // If there are, remove them.
            if(user.equals(userList.get(i))){
                updated = true;
                userList.remove(i);
                break;
            }
        }
        //
    }

    /**
     * Adds a friend using the HttpClient and calls to the callback
     * @param user the user to add
     * @param callback the callback that is called
     */
    public void addFriendToList(User user, Callback callback){
        if(outgoingRequests.contains(user))
            return;

        try{
            JSONObject object = new JSONObject();
            object.put("friendID" , user.getUUID().toString());
            HttpClient.getInstance().post(NetworkUtilities.HTTP_URL + "/user/friends", object, callback);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void deleteFriend(User user, Callback callback){
        try{
            JSONObject object = new JSONObject();
            object.put("friendID" , user.getUUID().toString());
            HttpClient.getInstance().delete(NetworkUtilities.HTTP_URL + "/user/friends", object, callback);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void addFriendToList(User user){
        if(outgoingRequests.contains(user))
            return;

        Callback callback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {

            }
        };

        try{
            JSONObject object = new JSONObject();
            object.put("friendID" , user.getUUID().toString());
            HttpClient.getInstance().delete(NetworkUtilities.HTTP_URL + "/user/friends", object, callback);
        } catch (JSONException e){
            e.printStackTrace();
        }
        if(incomingRequests.contains(user)) {
            synchronized (friends) {
                friends.add(user);
            }
        }
    }

    /**
     * Load friends from the file @ FILE_NAME
     * returns true if successful, false if otherwise
     *
     */
    private static boolean readFromDisk(){
        File file = new File(FILE_NAME);

        if(file.exists()){
            FileInputStream is;

            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException e){
                e.printStackTrace();
                return false;
            }

            try{
                ObjectInputStream inputStream = new ObjectInputStream(is);
                instance = ((FriendsList) inputStream.readObject());
            } catch (Exception e){
                // TODO : see if we need to handle IOException and ClassNotFound exception differently
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * This will output the FriendsList to disk with the Serializable interface.
     */
    public void writeToDisk(){
        FileOutputStream os;
        // TODO : see if we need anything special to overwrite.
        try {
            File file = new File(FILE_NAME);
            if(file.exists())
                file.delete();
            os = new FileOutputStream(file);

        } catch (FileNotFoundException e){
            e.printStackTrace();
            return;
        }

        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(os);
            outputStream.writeObject(this);
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException in writeToDisk", e);
            return;
        }

        Log.d(LOG_TAG , "Write successful.");
    }

    // writes the object using an output stream
    private void writeObject(ObjectOutputStream out) throws IOException{
        // Tell us how many
        outputList(friends, out);
        outputList(incomingRequests, out);
        outputList(outgoingRequests, out);

    }

    private void outputList(List<User> userList, ObjectOutputStream out) throws IOException{
        out.writeInt(friends.size());

        for(int i = 0; i < friends.size(); i++) {
            out.writeObject(friends.get(i));
        }
    }

    // reads an object using an output stream
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
        friends = inputList(in);
        incomingRequests = inputList(in);
        outgoingRequests = inputList(in);

    }

    private List<User> inputList(ObjectInputStream in) throws IOException, ClassNotFoundException{
        int size = in.readInt();

        List<User> userList = new ArrayList<>();

        for(int i = 0; i < size; i++){
            userList.add(((User) in.readObject()));
        }

        return userList;
    }

    /**
     * Returns the underlying list of friends
     * @return friends - the underlying friends list
     */
    public List<User> getFriends(){
        return friends;
    }

    /**
     * This function returns a boolean indicating whether a user is a friend or not
     * @param user the user to check against the friends list
     * @return isFriend true if the user is a friend, false otherwise
     */
    public boolean isFriend(User user){
        return friends.contains(user);
    }

    public List<User> getIncomingRequests(){
        return incomingRequests;
    }

    public List<User> getOutgoingRequests(){
        return outgoingRequests;
    }

    private Runnable checkFriendStatusRunnable = new Runnable() {
        @Override
        public void run() {
            checkFriendStatus();
//            handler.postDelayed()
        }
    };

    /**
     * This method checks the status of a user's friends list to see if any have joined or left
     * Unisong sessions.
     */
    private void checkFriendStatus(){

    }

    public void destroy(){
        instance = null;

        friends = null;

        activeUsers = null;
        client = null;
        idleUsers = null;
        incomingRequests = null;
        outgoingRequests = null;
    }


    public void setRefreshLayout(SwipeRefreshLayout refreshLayout){
        this.swipeRefreshLayout = refreshLayout;
    }

    public void setFriendsAdapter(FriendsAdapter adapter){
        this.adapter = adapter;
    }
}
