package com.ezturner.speakersync.network.user;

import java.util.List;

/**
 * Created by Ethan on 8/9/2015.
 */
public class FriendsList {


    private List<User> mFriends;
    private List<FriendRequest> mRequests;

    public FriendsList(List<User> friends, List<FriendRequest> requests){
        mFriends = friends;
        mRequests = requests;
    }

    public boolean isAFriend(User user){
        for(User friend : mFriends){
            if(friend.equals(user)){
                return true;
            }
        }
        return false;
    }


}
