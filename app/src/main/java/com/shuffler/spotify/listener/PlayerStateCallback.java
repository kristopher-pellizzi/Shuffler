package com.shuffler.spotify.listener;

import com.shuffler.MainActivity;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.PlayerState;

public class PlayerStateCallback implements CallResult.ResultCallback<PlayerState> {

    private MainActivity main;

    public PlayerStateCallback(MainActivity activity){
        main = activity;
    }

    @Override
    public void onResult(PlayerState playerState) {
        main.updatePlayerState(playerState);
    }
}
