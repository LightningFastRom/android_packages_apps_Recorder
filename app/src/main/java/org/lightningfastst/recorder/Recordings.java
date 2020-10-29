package org.lightningfastst.recorder;

import android.os.Environment;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.recyclerview.widget.ListAdapter;

public class Recordings extends BaseAdapter {
    private final Activity context;
    private ArrayList mRecordings; //data source of the list adapter
    public Recordings(Activity context,
                      ArrayList mRecordings) {
        //super(context, R.layout.recording_item, recordings);
        this.context = context;
        this.mRecordings = mRecordings;
    }
    @Override
    public int getCount() {
        return mRecordings.size(); //returns total of items in the list
    }

    @Override
    public Object getItem(int position) {
        return mRecordings.get(position); //returns list item at the specified position
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.recording_item, null, true);
        TableLayout recording = (TableLayout) rowView.findViewById(R.id.recording);
        if (position % 2 == 1) {
            recording.setBackgroundColor(R.color.white);
        }else {
            recording.setBackgroundColor(R.color.colorPrimaryDark);
        }
        TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);
        txtTitle.setText(mRecordings.get(position).toString());
        return rowView;
    }
}

