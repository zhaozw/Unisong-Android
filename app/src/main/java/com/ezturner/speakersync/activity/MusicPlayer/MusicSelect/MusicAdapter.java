package com.ezturner.speakersync.activity.MusicPlayer.MusicSelect;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ezturner.speakersync.R;

import java.util.ArrayList;

/**
 * Created by Ethan on 2/26/2015.
 */
class MusicAdapter extends RecyclerView.Adapter <MusicAdapter.MusicViewHolder>
{
    private LayoutInflater mInflater;
    private ArrayList<MusicViewHolder> mViewHolders;
    private ArrayList<MusicData> mMusicData;


    public MusicAdapter(Context context){
        mInflater = LayoutInflater.from(context);
    }

    public void setData(ArrayList<MusicData> musicData){
        mMusicData = musicData;

    }



    @Override
    public MusicViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.drawer_row , viewGroup , false);

        MusicViewHolder holder = new MusicViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(MusicViewHolder viewHolder, int i) {


    }

    @Override
    public int getItemCount() {
        return mMusicData.size();
    }

    class MusicViewHolder extends RecyclerView.ViewHolder{

        private TextView mPrimaryText;
        private TextView mSecondaryText;
        private ImageView mImage;

        public MusicViewHolder(View itemView) {
            super(itemView);

            mSecondaryText = (TextView) itemView.findViewById(R.id.grid_secondary_text);
            mPrimaryText = (TextView) itemView.findViewById(R.id.grid_primary_text);
            mImage = (ImageView) itemView.findViewById(R.id.grid_image_view);

        }
    }
}