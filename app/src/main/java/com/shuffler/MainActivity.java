package com.shuffler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.shuffler.broadcast.receiver.NetworkStateChangeListener;
import com.shuffler.service.EnqueueingService;
import com.shuffler.spotify.listener.CapabilitiesCallback;
import com.shuffler.spotify.listener.ConnectionListener;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Capabilities;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import java.nio.channels.Channels;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// TODO: add splash screen
// TODO: avoid that the background task is closed when the main activity is closed by either the user or the system
// TODO: add memory and disk caches to avoid loading playlists and tracks from the web api each time the App is opened
// TODO: if the enqueueing service is already active, directly allow user to interrupt the service
public class MainActivity extends AppCompatActivity {
    private String CLIENT_ID;
    private String REDIRECT_CALLBACK;
    private static SpotifyAppRemote mSpotifyAppRemote = null;
    private Integer requestCode;
    private ConnectionListener connectionListener;
    private ConnectionParams params;
    private TextView mainMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CLIENT_ID = getResources().getString(R.string.client_id);
        REDIRECT_CALLBACK = getResources().getString(R.string.redirect_callback);
        mainMessage = (TextView) findViewById(R.id.main_message);
        connectionListener = new ConnectionListener(this);
        params = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_CALLBACK)
                .showAuthView(true)
                .build();

        BroadcastReceiver br = new NetworkStateChangeListener();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(br, filter);
    }

    public void connect(){
        mainMessage.setText(R.string.main_activity_bootstrap);
        if(checkConnection()) {
            SpotifyAppRemote.connect(this, params, connectionListener);
        }
        else{
            mainMessage.setText(R.string.no_connection_available);
        }
    }

    private boolean checkConnection(){
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }

    // TODO: check if SerializableAppRemote constructor or passing through intent causes exceptions
    public void setSpotifyAppRemote(SpotifyAppRemote mSpotifyAppRemote){
        this.mSpotifyAppRemote = mSpotifyAppRemote;
    }

    private void checkPremiumSubscription(){
        mainMessage.setText(R.string.checking_for_premium_subscription);
        mSpotifyAppRemote.getUserApi().getCapabilities().setResultCallback(new CapabilitiesCallback(this));
    }

    public void checkPremiumSubscription(Capabilities capabilities){
        if(!capabilities.canPlayOnDemand)
            mainMessage.setText(R.string.no_premium_subscription);
        else
            getSpotifyAuthToken();
    }

    public void connected(){
        mainMessage.setText(R.string.connected);
        checkPremiumSubscription();
    }

    private void getSpotifyAuthToken(){
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_CALLBACK)
                .setScopes(new String[]{"playlist-read-private", "playlist-read-collaborative"});
        AuthenticationRequest request = builder.build();
        requestCode = Integer.parseInt(getResources().getString(R.string.auth_request_code));
        AuthenticationClient.openLoginActivity(this, requestCode, request);
    }

    public void connectionFailed(Throwable throwable){
        StringBuilder sb = new StringBuilder(getResources().getString(R.string.connection_failed));
        sb.append('\n');
        sb.append("Error message: ");
        sb.append(throwable.getMessage());

        mainMessage.setText(sb.toString());
    }


    public static SpotifyAppRemote getAppRemote(){
        return mSpotifyAppRemote;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == this.requestCode) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            StringBuilder sb;

            switch (response.getType()) {
                case TOKEN:
                    String authToken = response.getAccessToken();
                    mainMessage.setText(R.string.wait_for_track_loading);
                    Intent service = new Intent(this, EnqueueingService.class)
                            .putExtra("token", authToken);
                    startService(service);
                    mainMessage.setText(R.string.service_launched);
                    Executors.newSingleThreadScheduledExecutor().schedule(
                            new Runnable() {

                                @Override
                                public void run() {
                                    finish();
                                }
                            },

                            3,

                            TimeUnit.SECONDS
                    );
                    break;

                case ERROR:
                    sb = new StringBuilder(getResources().getString(R.string.auth_error_response))
                            .append('\n')
                            .append("Error: \n")
                            .append(response.getError());
                    mainMessage.setText(sb.toString());
                    break;

                default:
                    sb = new StringBuilder(getResources().getString(R.string.default_response_type_switch))
                            .append('\n')
                            .append("HTTP response code: \n")
                            .append(response.getCode());
                    mainMessage.setText(sb.toString());

            }
        }
    }

}
