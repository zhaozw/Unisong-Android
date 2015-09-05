package io.unisong.android.activity.MusicPlayer;

/**
 * Created by ezturner on 8/7/2015.
 */
public class DrawerInformation {

    private String mIcon;
    private String mText;

    public DrawerInformation(String icon, String text){
        mIcon = icon;
        mText = text;
    }

    public String getIcon(){
        return mIcon;
    }

    public String getText(){
        return mText;
    }

    public void setIcon(String icon){
        mIcon = icon;
    }

    public void setText(String text){
        mText = text;
    }
}
