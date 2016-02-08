package io.unisong.android.network.user;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generic util
 * Created by Ethan on 1/7/2016.
 */
public class UserUtils {

    private final static String LOG_TAG = UserUtils.class.getSimpleName();
    private static List<User> users = new ArrayList<>();

    public static void addUser(User user){
        if(!users.contains(user))
            users.add(user);
    }

    public static User getUser(UUID uuid){
        synchronized (users) {
            for (User user : users) {
                if (user.getUUID() != null && user.getUUID().equals(uuid)) {
                    return user;
                }
            }
        }

        User user = new User(uuid);
        synchronized (users) {
            users.add(user);
        }

        return user;
    }

    public static User getUser(String uuid){
        return getUser(UUID.fromString(uuid));
    }

    /**
     * This method returns the User matching the phone number provided
     * @param formattedPhoneNumber - an E164 formatted phone number that the user used to register
     * @return user the user matching the phone number, nullable
     */
    @Nullable
    public static User getUserByPhone(String formattedPhoneNumber){
        for(User user : users){
            if(user.getPhoneNumber().equals(formattedPhoneNumber)){
                return user;
            }
        }

        return null;
    }

}
