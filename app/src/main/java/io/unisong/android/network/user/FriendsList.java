package io.unisong.android.network.user;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

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

    private static FriendsList sInstance;

    public static FriendsList getInstance(){
        return sInstance;
    }

    private HttpClient mClient;
    private List<User> mFriends;
    private List<User> mIncomingRequests;
    private List<User> mOutgoingRequests;

    // Users not currently in an Unisong session.
    private List<User> mIdleUsers;

    // Users currently in an active Unisong session.
    private List<User> mActiveUsers;

    private Context mContext;

    private Thread mFriendsThread;

    // This tells us whether the data has been updated and needs to be written to disk
    private boolean mUpdated;
    private Handler mHandler;

    /**
     * Instantiates the FriendsList, and loads the relevant data from disk if available
     * and the network if not.
     * Also checks to see if the data on disk is up to date.
     */
    public FriendsList(Context context){
        sInstance = this;
        mUpdated = false;
        mContext = context;

        // TODO : handle not being logged in and having no data on disk.
        // TODO : implement storage with file system not Prefs.

        // these will be overwritten if read from the disk
        mFriends = new ArrayList<>();
        mIncomingRequests = new ArrayList<>();
        mOutgoingRequests = new ArrayList<>();

        mHandler = new Handler();

        mClient = HttpClient.getInstance();
        // TODO : get friends from server.
        // TODO : store and only update.
        // TODO : reenable after testing FB integration.


        loadFriends();

    }

    private void loadFriends(){

        mFriendsThread = getFriendsThread();
        mFriendsThread.start();
    }


    private void waitForLogin(){
        // Wait until we're logged in and have a CurrentUser to get info from.
        // TODO : can the httpclient be null here? Are there situations in which we should handle this?
        while(!mClient.isLoginDone() && !mClient.isLoggedIn()){
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



    public boolean isAFriend(User user){
        for(User friend : mFriends){
            if(friend.equals(user)){
                return true;
            }
        }
        return false;
    }

    private Thread getFriendsThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {

                waitForLogin();

                loadFriendsFromServer();

                mHandler.post(mCheckFriendStatusRunnable);

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
            JSONArray inReqArray = body.getJSONArray("incomingRequests");
            JSONArray outReqAr = body.getJSONArray("outgoingRequests");

            // Load friends
            for(int i = 0; i < friendsArray.length(); i++){

                JSONObject object = friendsArray.getJSONObject(i);
                User userToAdd = new User(UUID.fromString(object.getString("userID")));
                boolean isEqual = false;

                // Make sure the user we're loading isn't already in mFriends
                for(User user : mFriends){
                    if(user.equals(userToAdd)){
                        isEqual = true;
                        break;
                    }
                }

                // remove any duplicates and then add
                if(!isEqual) {
                    mUpdated = true;
                    removeDuplicateUsers(userToAdd , mIncomingRequests);
                    removeDuplicateUsers(userToAdd , mOutgoingRequests);
                    mFriends.add(userToAdd);
                }
            }

            for(int i = 0; i < inReqArray.length(); i++){

                JSONObject object = inReqArray.getJSONObject(i);
                User userToAdd = new User(UUID.fromString(object.getString("userID")));
                boolean isEqual = false;

                for(User user : mIncomingRequests){
                    if(user.equals(userToAdd)){
                        isEqual = true;
                        break;
                    }
                }

                // remove any duplicates and then add
                if(!isEqual) {
                    mUpdated = true;
                    removeDuplicateUsers(userToAdd , mOutgoingRequests);
                    removeDuplicateUsers(userToAdd , mFriends);
                    mIncomingRequests.add(userToAdd);
                }
            }

            for(int i = 0; i < outReqAr.length(); i++){

                JSONObject object = outReqAr.getJSONObject(i);
                User userToAdd = new User(UUID.fromString(object.getString("userID")));
                boolean isEqual = false;

                // make sure we dont' already have this user.
                for(User user : mOutgoingRequests){
                    if(user.equals(userToAdd)){
                        break;
                    }
                }

                // remove any duplicates and then add
                if(!isEqual) {
                    mUpdated = true;
                    removeDuplicateUsers(userToAdd , mIncomingRequests);
                    removeDuplicateUsers(userToAdd , mFriends);
                    mOutgoingRequests.add(userToAdd);
                }
            }

            if(mUpdated){
                // TODO : enable caching.
                //writeToDisk();
            }
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
                mUpdated = true;
                userList.remove(i);
                break;
            }
        }
        //
    }

    // TODO : implement
    public void addFriend(User user){

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
                sInstance = ((FriendsList) inputStream.readObject());
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
        outputList(mFriends, out);
        outputList(mIncomingRequests, out);
        outputList(mOutgoingRequests, out);

    }

    private void outputList(List<User> userList, ObjectOutputStream out) throws IOException{
        out.writeInt(mFriends.size());

        for(int i = 0; i < mFriends.size(); i++) {
            out.writeObject(mFriends.get(i));
        }
    }

    // reads an object using an output stream
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{

        mFriends = inputList(in);
        mIncomingRequests = inputList(in);
        mOutgoingRequests = inputList(in);

    }

    private List<User> inputList(ObjectInputStream in) throws IOException, ClassNotFoundException{
        int size = in.readInt();

        List<User> userList = new ArrayList<>();

        for(int i = 0; i < size; i++){
            userList.add(((User) in.readObject()));
        }

        return userList;
    }

    public List<User> getFriends(){
        return mFriends;
    }

    public List<User> getIncomingRequests(){
        return mIncomingRequests;
    }

    public List<User> getOutgoingRequests(){
        return mOutgoingRequests;
    }

    private Runnable mCheckFriendStatusRunnable = new Runnable() {
        @Override
        public void run() {
            checkFriendStatus();
//            mHandler.postDelayed()
        }
    };

    /**
     * This method checks the status of a user's friends list to see if any have joined or left
     * Unisong sessions.
     */
    private void checkFriendStatus(){

    }

    public void destroy(){
        sInstance = null;

        mFriends = null;

        mActiveUsers = null;
        mClient = null;
        mIdleUsers = null;
        mIncomingRequests = null;
        mOutgoingRequests = null;
    }
}
