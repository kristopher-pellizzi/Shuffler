package com.shuffler.volley.listener;

import com.android.volley.Response;
import com.shuffler.MainActivity;
import org.json.JSONObject;


public class PlaylistsResponseListener implements Response.Listener<JSONObject> {

    private MainActivity main;

    public PlaylistsResponseListener(MainActivity activity){
        main = activity;
    }

    @Override
    public void onResponse(JSONObject response) {
        main.managePlaylistResponse(response);
    }
}
