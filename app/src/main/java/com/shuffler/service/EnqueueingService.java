package com.shuffler.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;


import com.shuffler.R;
import com.shuffler.utility.ServiceWorker;

import java.util.concurrent.ThreadLocalRandom;


public class EnqueueingService extends Service {

    public EnqueueingService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String authToken = intent.getStringExtra("token");

        Notification notification = new NotificationCompat.Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.enqueueing_service_notification_text))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getResources().getString(R.string.enqueueing_service_notification_big_text)))
                .build();
        startForeground(ThreadLocalRandom.current().nextInt(), notification);

        new ServiceWorker(this, authToken).start();
        return START_STICKY;
    }

    // TODO: implement onBind callback
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
