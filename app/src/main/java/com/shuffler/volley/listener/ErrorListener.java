package com.shuffler.volley.listener;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.shuffler.handler.RequestHandler;

import java.io.Serializable;

public class ErrorListener implements Response.ErrorListener {

    private RequestHandler handler;

    public ErrorListener(RequestHandler handler){
        this.handler = handler;
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        handler.manageWebRequestError(error);
    }
}
