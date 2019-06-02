package com.shuffler.handler;

import com.android.volley.NoConnectionError;

public interface VolleyErrorHandler {
    void handle(NoConnectionError e);
}
