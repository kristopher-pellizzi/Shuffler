package com.shuffler.broadcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.shuffler.appcontext.AppContext;

public class NetworkStateChangeListener extends BroadcastReceiver {

    public NetworkStateChangeListener(AppContext context){
        // Do nothing. This constructor is only useful to avoid creating such a kind of listener using a not suitable context
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        AppContext main = (AppContext) context;
        main.connect();
    }
}
