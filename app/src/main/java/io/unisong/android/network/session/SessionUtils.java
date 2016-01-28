package io.unisong.android.network.session;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 1/16/2016.
 */
public class SessionUtils {

    private final static String LOG_TAG = SessionUtils.class.getSimpleName();

    private static List<UnisongSession> sessions = new ArrayList<>();

    /**
     * Retrieves the session with the selected ID if it exists,
     * creates it if it doesn't
     * @param sessionID - the ID of the session to be returned
     * @return UnisongSession session
     */
    public static UnisongSession getSessionByID(int sessionID){

        UnisongSession session;
        synchronized (sessions) {
            // check to see if we have the session
            for (UnisongSession sess : sessions) {
                if (sess.getSessionID() == sessionID) {
                    return sess;
                }
            }

            Log.d(LOG_TAG, "No session with ID #" + sessionID + " found. Creating a new one.");

            session = new UnisongSession(sessionID);
            sessions.add(session);

        }
        return session;
    }

    /**
     * Remove the session with the selected ID from sessions
     * @param sessionID
     */
    public static void removeSession(int sessionID){

        synchronized (sessions) {
            UnisongSession toRemove = null;
            for (UnisongSession session : sessions) {
                if (session.getSessionID() == sessionID) {
                    toRemove = session;
                }
            }

            if (toRemove != null)
                sessions.remove(toRemove);
        }
    }
}
