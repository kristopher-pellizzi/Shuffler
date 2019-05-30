package com.shuffler.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;


import com.shuffler.MainActivity;
import com.shuffler.R;

import java.util.concurrent.ThreadLocalRandom;


public class EnqueueingService extends Service {

    public static boolean isRunning;
    public static ServiceWorker worker;

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

        isRunning = true;

        worker = new ServiceWorker(this, authToken);
        worker.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        worker.interrupt();
        stopForeground(true);
        super.onDestroy();
    }

    // TODO: implement onBind callback
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
