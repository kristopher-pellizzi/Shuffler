package com.shuffler.spotify.listener;

import com.shuffler.handler.QueueRequestHandler;
import com.spotify.protocol.client.ErrorCallback;

public class QueueErrorCallback implements ErrorCallback {

    private QueueRequestHandler handler;
    private String track;

    public QueueErrorCallback(QueueRequestHandler handler, String track){
        this.handler = handler;
        this.track = track;
    }

    @Override
    public void onError(Throwable throwable) {
        handler.failedQueueRequest(track);
    }
}
