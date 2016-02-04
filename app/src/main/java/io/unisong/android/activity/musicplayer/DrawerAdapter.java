package io.unisong.android.activity.musicplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.thedazzler.droidicon.IconicFontDrawable;

import java.util.List;

import io.unisong.android.R;

/**
 * Created by Work on 4/9/2015.
 */
public class DrawerAdapter extends RecyclerView.Adapter <DrawerAdapter.DrawerViewHolder>{

    private static final String LOG_TAG = DrawerAdapter.class.getSimpleName();

    private LayoutInflater inflater;
    private List<DrawerInformation> data;
    private Context context;

    public DrawerAdapter(Context context, List<DrawerInformation> data){
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.data = data;
    }

    @Override
    public DrawerViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = inflater.inflate(R.layout.drawer_row , viewGroup , false);

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
        String text = data.get(i).getText();
        String icon = data.get(i).getIcon();

        //Set the text and icon
        viewHolder.mText.setText(text);
        viewHolder.mImage.setTag(i);


        IconicFontDrawable iconicFontDrawable = new IconicFontDrawable(context);
        iconicFontDrawable.setIcon(icon);
        iconicFontDrawable.setIconColor(context.getResources().getColor(R.color.colorAccent));

        final float scale = context.getResources().getDisplayMetrics().density;
        int pixels = (int) (context.getResources().getDimension(R.dimen.drawer_icon_size) * scale + 0.5f);

        //TODO: find a way to adjust this if necessary
        //OR...set the width and height to the icon and use it as bounded drawables
        iconicFontDrawable.setIntrinsicWidth(pixels);
        iconicFontDrawable.setIntrinsicHeight(pixels);
        //setImageDrawable to ImageView
        viewHolder.mImage.setImageDrawable(iconicFontDrawable);
    }

    @Override
    public int getItemCount() {
        return data.size();
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
