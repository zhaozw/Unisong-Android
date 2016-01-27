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

    /**
     * Retrieves the session with the selected ID if it exists,
     * creates it if it doesn't
     * @param sessionID - the ID of the session to be returned
     * @return UnisongSession session
     */
    public static UnisongSession getSessionByID(int sessionID){

        UnisongSession session;
        synchronized (sSessions) {
            // check to see if we have the session
            for (UnisongSession sess : sSessions) {
                if (sess.getSessionID() == sessionID) {
                    return sess;
                }
            }

            Log.d(LOG_TAG, "No session with ID #" + sessionID + " found. Creating a new one.");

            session = new UnisongSession(sessionID);
            sSessions.add(session);

        }
        return session;
    }

    /**
     * Remove the session with the selected ID from sSessions
     * @param sessionID
     */
    public static void removeSession(int sessionID){

        synchronized (sSessions) {
            UnisongSession toRemove = null;
            for (UnisongSession session : sSessions) {
                if (session.getSessionID() == sessionID) {
                    toRemove = session;
                }
            }

            if (toRemove != null)
                sSessions.remove(toRemove);
        }
    }
}
