package com.ezturner.speakersync.activity.musicplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ezturner.speakersync.R;

/**
 * Created by Work on 4/9/2015.
 */
public class DrawerAdapter extends RecyclerView.Adapter <DrawerAdapter.DrawerViewHolder>{

    private LayoutInflater mInflater;


    public DrawerAdapter(Context context){

        mInflater = LayoutInflater.from(context);
    }
    @Override
    public DrawerViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.drawer_row , viewGroup , false);

        DrawerViewHolder holder = new DrawerViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(DrawerViewHolder viewHolder, int i) {
        String title = "";
        int id = 0;
        if(i == 0){
            title = "My Library";
            id = R.drawable.ic_my_library_music;
        } else if(i == 1){
            title = "Settings";
            id = R.drawable.ic_settings_black;
        }
        viewHolder.mText.setText(title);
        viewHolder.mImage.setImageResource(id);
        viewHolder.mImage.setTag(i);
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    class DrawerViewHolder extends RecyclerView.ViewHolder{

        private TextView mText;
        private ImageView mImage;

        public DrawerViewHolder(View itemView) {
            super(itemView);

            mText = (TextView) itemView.findViewById(R.id.listText);

            mImage = (ImageView) itemView.findViewById(R.id.listImage);

        }
    }

}
