package io.unisong.android.network.session;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 1/16/2016.
 */
public class SessionUtils {

    private final static String LOG_TAG = SessionUtils.class.getSimpleName();

    private static List<UnisongSession> sSessions = new ArrayList<>();

    public static UnisongSession getSessionByID(int sessionID){

        for(UnisongSession session : sSessions){
            if(session.getSessionID() == sessionID){
                return session;
            }
        }

        Log.d(LOG_TAG, "No session with ID #" + sessionID + " found. Creating a new one.");

        UnisongSession session = new UnisongSession(sessionID);
        sSessions.add(session);

        return session;
    }

    public static void removeSession(int sessionID){
        UnisongSession toRemove = null;
        for(UnisongSession session : sSessions){
            if(session.getSessionID() == sessionID){
                toRemove = session;
            }
        }

        if(toRemove != null)
            sSessions.remove(toRemove);
    }
}
