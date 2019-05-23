package com.shuffler.volley.listener;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.shuffler.MainActivity;

public class ErrorListener implements Response.ErrorListener {

    private MainActivity main;

    public ErrorListener(MainActivity activity){
        main = activity;
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        main.manageWebRequestError(error);
    }
}
