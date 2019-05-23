package com.shuffler.spotify.listener;

import com.shuffler.MainActivity;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

public class PlayerStateListener implements Subscription.EventCallback<PlayerState> {

    private MainActivity main;

    public PlayerStateListener(MainActivity activity){
        main = activity;
    }

    @Override
    public void onEvent(PlayerState playerState) {
            main.updatePlayerState(playerState);
    }
}
