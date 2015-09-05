package io.unisong.android.activity.MusicPlayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.unisong.android.R;

import com.thedazzler.droidicon.IconicFontDrawable;

import java.util.List;

/**
 * Created by Work on 4/9/2015.
 */
public class DrawerAdapter extends RecyclerView.Adapter <DrawerAdapter.DrawerViewHolder>{

    private static final String LOG_TAG = DrawerAdapter.class.getSimpleName();

    private LayoutInflater mInflater;
    private List<DrawerInformation> mData;
    private Context mContext;

    public DrawerAdapter(Context context, List<DrawerInformation> data){
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mData = data;
    }

    @Override
    public DrawerViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.drawer_row , viewGroup , false);

        DrawerViewHolder holder = new DrawerViewHolder(view);

        return holder;
    }

    /**
     * Sets the image and text for a navigation drawer row
     * @param viewHolder - The R.layout.drawer_row
     * @param i - The # row it is, starting at 0
     */
    @Override
    public void onBindViewHolder(DrawerViewHolder viewHolder, int i) {

        //Get the values from DrawerInformation
        String text = mData.get(i).getText();
        String icon = mData.get(i).getIcon();

        //Set the text and icon
        viewHolder.mText.setText(text);
        viewHolder.mImage.setTag(i);


        IconicFontDrawable iconicFontDrawable = new IconicFontDrawable(mContext);
        iconicFontDrawable.setIcon(icon);
        iconicFontDrawable.setIconColor(mContext.getResources().getColor(R.color.accent));

        final float scale = mContext.getResources().getDisplayMetrics().density;
        int pixels = (int) (mContext.getResources().getDimension(R.dimen.drawer_icon_size) * scale + 0.5f);

        //TODO: find a way to adjust this if necessary
        //OR...set the width and height to the icon and use it as bounded drawables
        iconicFontDrawable.setIntrinsicWidth(pixels);
        iconicFontDrawable.setIntrinsicHeight(pixels);
        //setImageDrawable to ImageView
        viewHolder.mImage.setImageDrawable(iconicFontDrawable);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    class DrawerViewHolder extends RecyclerView.ViewHolder{

        private TextView mText;
        private ImageView mImage;

        public DrawerViewHolder(View itemView) {
            super(itemView);

            mText = (TextView) itemView.findViewById(R.id.drawerRowText);

            mImage = (ImageView) itemView.findViewById(R.id.drawerRowImage);

        }
    }

}
