package io.unisong.android.network.user;

/**
 * Created by Ethan on 8/9/2015.
 */
public class FriendRequest {

    private User mUser;
    public FriendRequest(User userfrom){
        mUser = userfrom;
    }

    public User getUserFrom(){
        return mUser;
    }

    /**
     * The method to accept a friend request
     */
    public void accept(){
        //TODO: send an http request accepting
    }

    public void decline(){
        //TODO: send an http request declining
    }
}
