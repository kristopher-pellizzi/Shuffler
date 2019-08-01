package com.shuffler.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


import com.shuffler.activity.MainActivity;
import com.shuffler.R;
import com.shuffler.appcontext.AppContext;
import com.shuffler.broadcast.receiver.NetworkStateChangeListener;
import com.shuffler.utility.ConnectivityChecker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;


public class EnqueueingService extends Service implements AppContext {

    public static boolean isRunning;
    public static ServiceWorker worker;
    // tracksInfo will contain a couple <author, title> for each track, identified by their uri
    public static Map<String, Pair<String, String>> tracksInfo;
    private BroadcastReceiver br = null;
    private Integer notificationID = null;

    public EnqueueingService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String authToken = intent.getStringExtra("token");

        Intent activityIntent = new Intent(this, MainActivity.class);
        Notification notification = new NotificationCompat.Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.enqueueing_service_notification_text))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getResources().getString(R.string.enqueueing_service_notification_big_text)))
                .setContentIntent(PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .build();
        startForeground(ThreadLocalRandom.current().nextInt(), notification);

        tracksInfo = new HashMap<>();
        isRunning = true;

        worker = new ServiceWorker(this, authToken);
        worker.start();
        return START_STICKY;
    }

    public void createReceiver(int notificationID){
        if(br == null){
            this.notificationID = notificationID;
            br = new NetworkStateChangeListener(this);
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(br, filter);
        }
    }

    @Override
    public void onDestroy() {
        worker.interrupt();
        stopForeground(true);
        super.onDestroy();
    }

    // This service is not thought to be bound. It won't accept requests by any application. It simply launches a thread listening for Spotify's player state changes and react to them
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void connect() {
        if(ConnectivityChecker.checkConnection(this)) {
            NotificationManagerCompat.from(this).cancel(notificationID);
            worker.resumeServiceWork(notificationID);
            notificationID = null;
            unregisterReceiver(br);
            br = null;
        }
    }
}
