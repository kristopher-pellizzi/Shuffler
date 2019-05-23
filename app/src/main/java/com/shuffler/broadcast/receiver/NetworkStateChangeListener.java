package com.shuffler.broadcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.shuffler.MainActivity;

public class NetworkStateChangeListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity main = (MainActivity) context;
        main.connect();
    }
}
