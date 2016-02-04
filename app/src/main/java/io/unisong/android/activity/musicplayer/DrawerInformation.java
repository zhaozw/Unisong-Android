package io.unisong.android.activity.musicplayer;

/**
 * Created by ezturner on 8/7/2015.
 */
public class DrawerInformation {

    private String icon;
    private String text;

    public DrawerInformation(String icon, String text){
        this.icon = icon;
        this.text = text;
    }

    public String getIcon(){
        return icon;
    }

    public String getText(){
        return text;
    }

    public void setIcon(String icon){
        this.icon = icon;
    }

    public void setText(String text){
        this.text = text;
    }
}
