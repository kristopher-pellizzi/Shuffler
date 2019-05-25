package com.shuffler.volley.listener;

import com.android.volley.Response;
import com.shuffler.handler.RequestHandler;

import org.json.JSONObject;


public class PlaylistsResponseListener implements Response.Listener<JSONObject> {

    private RequestHandler handler;

    public PlaylistsResponseListener(RequestHandler handler){
        this.handler = handler;
    }

    @Override
    public void onResponse(JSONObject response) {
        handler.managePlaylistResponse(response);
    }
}
