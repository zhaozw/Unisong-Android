package io.unisong.android.activity.musicselect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import io.unisong.android.R;
import io.unisong.android.network.user.User;

import java.net.URI;
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

        RelativeLayout layout = (RelativeLayout) viewHolder.mImage.getParent();

        String tag = data.getType() + ":"  + data.getID();
        layout.setTag(tag);

        viewHolder.mPrimaryText.setText(data.getPrimaryText());
        viewHolder.mSecondaryText.setText(data.getSecondaryText());
//        Log.d(LOG_TAG , data.getImageURL());
        if(data.getImageURL() != null && !data.getImageURL().equals("null") && !data.getImageURL().equals("")) {
            // TODO : figure out how to load these images and set a default one.
            try {
                String path = data.getImageURL();
                Log.d(LOG_TAG , path);
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                viewHolder.mImage.setImageBitmap(bitmap);
            } catch (Exception e){
                e.printStackTrace();
                Log.d(LOG_TAG , "Error on image load!");
            }
        }

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