package io.unisong.android.activity.musicselect;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import io.unisong.android.R;
import io.unisong.android.network.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ethan on 2/26/2015.
 */
class MusicAdapter extends RecyclerView.Adapter <MusicAdapter.MusicViewHolder>
{
    private final static String LOG_TAG = MusicAdapter.class.getSimpleName();
    private LayoutInflater mInflater;
    private List<MusicViewHolder> mViewHolders;
    private List<MusicData> mMusicData;


    public MusicAdapter(Context context){
        mInflater = LayoutInflater.from(context);
    }

    public void setData(List<MusicData> musicData){
        mMusicData = musicData;
    }



    @Override
    public MusicViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.music_row , viewGroup , false);

        MusicViewHolder holder = new MusicViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(MusicViewHolder viewHolder, int i) {

        MusicData data = mMusicData.get(i);
//        Log.d(LOG_TAG , data.getImageURL());
//        if(data.getImageURL() != null && !data.getImageURL().equals("null"))
//            Picasso.with(viewHolder.mImage.getContext()).load(data.getImageURL()).into((viewHolder.mImage));

        viewHolder.mPrimaryText.setText(data.getPrimaryText());
        viewHolder.mSecondaryText.setText(data.getSecondaryText());
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

            mSecondaryText = (TextView) itemView.findViewById(R.id.music_second_line);
            mPrimaryText = (TextView) itemView.findViewById(R.id.music_first_line);
            mImage = (ImageView) itemView.findViewById(R.id.music_image);

        }
    }
}