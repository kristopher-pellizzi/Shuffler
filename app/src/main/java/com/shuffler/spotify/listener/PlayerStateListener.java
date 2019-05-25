package com.shuffler.spotify.listener;

import com.shuffler.MainActivity;
import com.shuffler.handler.PlayerStateUpdateHandler;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

public class PlayerStateListener implements Subscription.EventCallback<PlayerState> {

    private PlayerStateUpdateHandler handler;

    public PlayerStateListener(PlayerStateUpdateHandler handler){
        this.handler = handler;
    }

    @Override
    public void onEvent(PlayerState playerState) {
            handler.updatePlayerState(playerState);
    }
}
