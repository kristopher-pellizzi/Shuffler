package com.shuffler.spotify.listener;

import com.shuffler.activity.MainActivity;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

public class ConnectionListener implements Connector.ConnectionListener{

    private MainActivity main;


    public ConnectionListener(MainActivity activity){
        this.main = activity;
    }

    @Override
    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
        main.setSpotifyAppRemote(spotifyAppRemote);
        main.connected();
    }

    @Override
    public void onFailure(Throwable throwable) {
        main.connectionFailed(throwable);
    }

}
