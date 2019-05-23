package com.shuffler.volley.listener;

import com.android.volley.Response;
import com.shuffler.MainActivity;

import org.json.JSONObject;

public class TrackResponseListener implements Response.Listener<JSONObject> {

    private MainActivity main;

    public TrackResponseListener(MainActivity activity){
        main = activity;
    }

    @Override
    public void onResponse(JSONObject response) {
        main.manageTrackResponse(response);
    }
}
