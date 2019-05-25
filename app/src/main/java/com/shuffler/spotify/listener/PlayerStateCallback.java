package com.shuffler.spotify.listener;

import com.shuffler.handler.PlayerStateUpdateHandler;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.PlayerState;

public class PlayerStateCallback implements CallResult.ResultCallback<PlayerState> {

    private PlayerStateUpdateHandler handler;

    public PlayerStateCallback(PlayerStateUpdateHandler handler){
        this.handler = handler;
    }

    @Override
    public void onResult(PlayerState playerState) {
        handler.updatePlayerState(playerState);
    }
}
