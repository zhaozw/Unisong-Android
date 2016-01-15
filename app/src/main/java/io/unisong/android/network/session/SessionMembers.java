package io.unisong.android.network.session;

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
    private SessionMembersAdapter mAdapter;


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

    public void registerAdapter(SessionMembersAdapter adapter){
        mAdapter = adapter;
    }

    public int indexOf(User user){
        return mMembers.indexOf(user);
    }

    public void add(User user){
        mMembers.add(user);

        if(mAdapter != null)
            mAdapter.notifyItemInserted(mMembers.size());
    }

    public void remove(User user){
        int index = mMembers.indexOf(user);

        mMembers.remove(user);

        if(mAdapter != null)
            mAdapter.notifyItemRemoved(index);
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
}
