package io.unisong.android.network.session;

import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import io.unisong.android.activity.session.SessionMembersAdapter;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;
import io.unisong.android.network.user.UserUtils;

/**
 * This class is essentially a wrapper for a List<User> that contains the local copy
 * of a session's members. This class will notify a SessionMembersAdapter of any changes to the
 * data model, thereby keeping the UI in sync
 * Created by ezturner on 1/15/2016.
 */
public class SessionMembers {

    private final static String LOG_TAG = SessionMembers.class.getSimpleName();

    private List<User> members;
    private UnisongSession parentSession;
    private SessionMembersAdapter.IncomingHandler handler;


    public SessionMembers(UnisongSession session){
        members = new ArrayList<>();
        parentSession = session;
    }

    public void add(User user){
        if(members.indexOf(user) != -1)
            return;

        members.add(user);
        sendAdd(members.indexOf(user), user);

    }

    public void remove(User user){
        int index = members.indexOf(user);

        if(index == -1)
            return;

        sendRemove(members.indexOf(user));
        members.remove(user);

    }

    public List<User> getList(){
        return members;
    }

    public void update(JSONArray array){
        try {
            for (int i = 0; i < array.length(); i++) {
                User user = UserUtils.getUser(array.getString(i));

                if(members.indexOf(user) == -1){
                    members.add(user);
                }
            }
        } catch (JSONException e){
            Log.d(LOG_TAG , "Retrieving userIDs from JSONArray failed in SessionMembers.update()");
        }

        if(parentSession == UnisongSession.getCurrentSession()){
            if(members.indexOf(CurrentUser.getInstance()) == -1)
                members.add(CurrentUser.getInstance());
        }

    }

    public void registerHandler(SessionMembersAdapter.IncomingHandler handler){
        this.handler = handler;
    }

    /**
     * Notifies the SessionMembersAdapter that we are adding a member
     * @param position
     * @param user
     */
    private void sendAdd(int position, User user){
        if(handler == null)
            return;

        Message message = new Message();

        message.what = SessionMembersAdapter.ADD;
        message.arg1 = position;
        message.obj = user;

        handler.sendMessage(message);
    }

    /**
     * Notifies the SessionMembersAdapter that we are removing a member
     * @param position
     */
    private void sendRemove(int position){
        if(handler == null)
            return;

        Message message = new Message();

        message.what = SessionMembersAdapter.REMOVE;
        message.arg1 = position;

        handler.sendMessage(message);
    }


    /**
     * Returns true if the given user is in the local session members list
     * @param user - the user to be checked
     * @return
     */
    public boolean contains(User user){
        return members.contains(user);
    }
}
