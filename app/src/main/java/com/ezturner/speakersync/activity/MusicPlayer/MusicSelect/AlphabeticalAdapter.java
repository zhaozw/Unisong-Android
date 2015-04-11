package com.ezturner.speakersync.activity.MusicPlayer.MusicSelect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.ezturner.speakersync.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by Ethan on 2/26/2015.
 */
class AlphabeticalAdapter extends ArrayAdapter<MusicData> implements SectionIndexer
{
    private HashMap<String, Integer> alphaIndexer;
    private String[] sections;

    public AlphabeticalAdapter(Context c, int resource, List<MusicData> data)
    {
        super(c, resource, data);
        alphaIndexer = new HashMap<String, Integer>();
        for (int i = 0; i < data.size(); i++)
        {
            String s = data.get(i).getName().substring(0, 1).toUpperCase();
            if (!alphaIndexer.containsKey(s))
                alphaIndexer.put(s, i);
        }

        Set<String> sectionLetters = alphaIndexer.keySet();
        ArrayList<String> sectionList = new ArrayList<String>(sectionLetters);
        Collections.sort(sectionList);
        sections = new String[sectionList.size()];
        for (int i = 0; i < sectionList.size(); i++)
            sections[i] = sectionList.get(i);
    }

    public int getPositionForSection(int section)
    {
        return alphaIndexer.get(sections[section]);
    }

    public int getSectionForPosition(int position)
    {
        for ( int i = sections.length - 1; i >= 0; i-- ) {
            if ( position >= alphaIndexer.get( sections[ i ] ) ) {
                return i;
            }
        }
        return 0;
    }

    public Object[] getSections()
    {
        return sections;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        MusicData musicData = getItem(position);
        String name = musicData.getName();
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.music_list_item, parent, false);
        }
        // Lookup view for data population
        TextView nameView = (TextView) convertView.findViewById(R.id.name);
        TextView subtextView = (TextView) convertView.findViewById(R.id.sub_text);
        // Populate the data into the template view using the data object
        nameView.setText(name);
        subtextView.setText(musicData.getSubText());

        //Set the tag so we can know what resource to get
        convertView.setTag(musicData.getID());

        if(musicData.getID() == 0){
            //Set album art
        }
        // Return the completed view to render on screen
        return convertView;
    }
}