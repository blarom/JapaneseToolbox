package com.japanesetoolboxapp.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.japanesetoolboxapp.R;

import java.util.ArrayList;
import java.util.List;

public class StructuresGridViewAdapter extends ArrayAdapter {
    private Context context;
    private int layoutResourceId;
    private List<Integer> imageIds = new ArrayList();

    public StructuresGridViewAdapter(Context context, int layoutResourceId, List<Integer> imageIds) {
        super(context, layoutResourceId, imageIds);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.imageIds = imageIds;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        ViewHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new ViewHolder();
            holder.image = row.findViewById(R.id.list_item_structures_grid_image);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        int imageId = imageIds.get(position);
        holder.image.setBackgroundResource(imageId);
        return row;
    }

    class ViewHolder {
        ImageView image;

    }


}