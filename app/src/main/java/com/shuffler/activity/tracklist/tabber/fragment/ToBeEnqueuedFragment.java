package com.shuffler.activity.tracklist.tabber.fragment;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shuffler.R;
import com.shuffler.activity.tracklist.tabber.TracklistRecViewAdapter;
import com.shuffler.service.EnqueueingService;
import com.shuffler.service.ServiceWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToBeEnqueuedFragment extends Fragment {

    private static ToBeEnqueuedFragment instance = null;
    private List<String> titles;
    private TracklistRecViewAdapter adapter;

    private ToBeEnqueuedFragment(){
        titles = new ArrayList<>();
        Map<String, Pair<String, String>> trackInfo = EnqueueingService.tracksInfo;
        for(String uri : ServiceWorker.getTrackList()) {
            Pair<String, String> val = trackInfo.get(uri);
            StringBuilder sb = new StringBuilder(val.first)
                    .append(" - ")
                    .append(val.second);
            titles.add(sb.toString());
        }
        adapter = new TracklistRecViewAdapter(titles);
    }

    public static ToBeEnqueuedFragment getInstance(){
        if(instance == null){
            instance = new ToBeEnqueuedFragment();
        }
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tracklist_layout, container, false);
        RecyclerView recView = view.findViewById(R.id.list);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(container.getContext());
        recView.setAdapter(adapter);
        recView.setLayoutManager(manager);
        return view;
    }
}
