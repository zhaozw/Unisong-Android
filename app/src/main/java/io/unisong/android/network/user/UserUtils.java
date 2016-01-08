package io.unisong.android.network.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generic util
 * Created by Ethan on 1/7/2016.
 */
public class UserUtils {

    private static List<User> sUsers = new ArrayList<>();

    public static void addUser(User user){
        sUsers.add(user);
    }

    public static User getUser(UUID uuid){
        for(User user : sUsers){
            if(user.getUUID().equals(uuid)){
                return user;
            }
        }

        User user = new User(uuid);
        sUsers.add(user);

        return user;
    }

}
