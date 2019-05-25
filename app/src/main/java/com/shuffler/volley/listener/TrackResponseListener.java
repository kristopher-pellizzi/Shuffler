package com.shuffler.volley.listener;

import com.android.volley.Response;
import com.shuffler.MainActivity;
import com.shuffler.handler.RequestHandler;

import org.json.JSONObject;

public class TrackResponseListener implements Response.Listener<JSONObject> {

    private RequestHandler handler;

    public TrackResponseListener(RequestHandler handler){
        this.handler = handler;
    }

    @Override
    public void onResponse(JSONObject response) {
        handler.manageTrackResponse(response);
    }
}
