package com.shuffler.activity.tracklist.tabber;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shuffler.R;

import java.util.ArrayList;
import java.util.List;

public class TracklistRecViewAdapter extends RecyclerView.Adapter<TracklistRecViewAdapter.TrackViewHolder> {

    private List<String> dataset = new ArrayList<>();
    private int[] colors = {Color.parseColor("#FFBEB7B7"), Color.parseColor("#FFFFFFFF")};

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView v = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.tracklist_textview, parent, false);
        TrackViewHolder vh = new TrackViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        TextView textView = holder.getTextView();
        textView.setText(dataset.get(position));
        textView.setBackgroundColor(colors[holder.getAdapterPosition() % 2]);
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    // TODO: create event handling mechanism to update the dataset and the shown list when the list changes (i.e. when methods 'enqueue' is called and songs are removed from list 'tracks')
    public void setDataset(List<String> dataset){
        this.dataset = dataset;
    }

    public class TrackViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;

        public TrackViewHolder(TextView v){
            super(v);
            textView = v;
        }

        public TextView getTextView(){
            return textView;
        }

    }
}
