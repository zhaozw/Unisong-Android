package io.unisong.android.network.session;

import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.unisong.android.activity.session.SessionMembersAdapter;
import io.unisong.android.network.user.User;
import io.unisong.android.network.user.UserUtils;

/**
 * Created by ezturner on 1/15/2016.
 */
public class SessionMembers {

    private final static String LOG_TAG = SessionMembers.class.getSimpleName();

    private List<User> mMembers;
    private SessionMembersAdapter.IncomingHandler mHandler;


    public SessionMembers(){
        mMembers = new ArrayList<>();
    }

    public SessionMembers(JSONArray members){
        this();

        try {
            for (int i = 0; i < members.length(); i++) {
                mMembers.add(UserUtils.getUser(members.getString(i)));
            }
        } catch (JSONException e){
            e.printStackTrace();
            Log.d(LOG_TAG, "JSONException in SessionMembers' initialization ");
        }
    }

    public void add(User user){
        if(mMembers.indexOf(user) != -1)
            return;

        mMembers.add(user);
        sendAdd(mMembers.indexOf(user) , user);

    }

    public void remove(User user){
        int index = mMembers.indexOf(user);

        if(index == -1)
            return;

        sendRemove(mMembers.indexOf(user));
        mMembers.remove(user);

    }

    public List<User> getList(){
        return mMembers;
    }

    public void update(JSONArray array){
        try {
            for (int i = 0; i < array.length(); i++) {
                User user = UserUtils.getUser(array.getString(i));

                if(mMembers.indexOf(user) == -1){
                    mMembers.add(user);
                }
            }
        } catch (JSONException e){
            Log.d(LOG_TAG , "Retrieving userIDs from JSONArray failed in SessionMembers.update()");
        }
    }

    public void registerHandler(SessionMembersAdapter.IncomingHandler handler){
        mHandler = handler;
    }

    private void sendAdd(int position, User user){
        if(mHandler == null)
            return;

        Message message = new Message();

        message.what = SessionMembersAdapter.ADD;
        message.arg1 = position;
        message.obj = user;

        mHandler.sendMessage(message);
    }

    private void sendRemove(int position){
        if(mHandler == null)
            return;

        Message message = new Message();

        message.what = SessionMembersAdapter.REMOVE;
        message.arg1 = position;

        mHandler.sendMessage(message);
    }
}
