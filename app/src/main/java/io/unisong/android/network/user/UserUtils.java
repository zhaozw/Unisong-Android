package io.unisong.android.network.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generic util
 * Created by Ethan on 1/7/2016.
 */
public class UserUtils {

    private final static String LOG_TAG = UserUtils.class.getSimpleName();
    private static List<User> sUsers = new ArrayList<>();

    public static void addUser(User user){
        sUsers.add(user);
    }

    public static User getUser(UUID uuid){
        synchronized (sUsers) {
            for (User user : sUsers) {
                if (user.getUUID() != null && user.getUUID().equals(uuid)) {
                    return user;
                }
            }
        }

        User user = new User(uuid);
        synchronized (sUsers) {
            sUsers.add(user);
        }

        return user;
    }

    public static User getUser(String uuid){
        return getUser(UUID.fromString(uuid));
    }

}
