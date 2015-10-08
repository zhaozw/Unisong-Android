package io.unisong.android.activity.musicselect.album;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import io.unisong.android.R;
import io.unisong.android.activity.musicselect.MusicData;

/**
 * Created by ezturner on 10/7/2015.
 */
public class AlbumAdapter extends RecyclerView.Adapter <AlbumAdapter.MusicViewHolder>
{
    private final static String LOG_TAG = AlbumAdapter.class.getSimpleName();
    private LayoutInflater mInflater;
    private List<MusicViewHolder> mViewHolders;
    private List<MusicData> mMusicData;


    public AlbumAdapter(Context context){
        mInflater = LayoutInflater.from(context);
    }

    public void setData(List<MusicData> musicData){
        mMusicData = musicData;
    }



    @Override
    public MusicViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.album_row , viewGroup , false);

        MusicViewHolder holder = new MusicViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(MusicViewHolder viewHolder, int i) {

        MusicData data = mMusicData.get(i);

        viewHolder.mPrimaryText.setText(data.getPrimaryText());
        viewHolder.mSecondaryText.setText(data.getSecondaryText());
        LinearLayout layout = (LinearLayout) viewHolder.mPrimaryText.getParent();

        String tag = data.getType() + ":"  + data.getID();
        layout.setTag(tag);


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