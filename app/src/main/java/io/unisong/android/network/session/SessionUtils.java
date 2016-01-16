package io.unisong.android.network.session;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 1/16/2016.
 */
public class SessionUtils {

    private static List<UnisongSession> sSessions = new ArrayList<>();

    public static UnisongSession getSessionByID(int sessionID){

        for(UnisongSession session : sSessions){
            if(session.getSessionID() == sessionID){
                return session;
            }
        }

        return new UnisongSession(sessionID);
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
