package com.shuffler.handler;

import com.android.volley.VolleyError;

import org.json.JSONObject;

public interface RequestHandler {
    void manageWebRequestError(VolleyError error);
    void managePlaylistResponse(JSONObject response);
    void manageTrackResponse(JSONObject response);
}
