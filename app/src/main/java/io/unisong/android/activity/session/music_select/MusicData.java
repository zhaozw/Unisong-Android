package io.unisong.android.activity.session.music_select;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by Ethan on 2/26/2015.
 */
public interface MusicData {

    int SONG = 0;
    int PLAYLIST = 1;
    int ARTIST = 2;
    int GENRE = 3;
    int ALBUM = 4;

    String getPrimaryText();

    String getSecondaryText();

    long getID();

    @Nullable
    String getImageURL();

    int getType();

    List<MusicData> getChildren();

}