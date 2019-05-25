package com.shuffler.handler;

import com.spotify.protocol.types.PlayerState;

public interface PlayerStateUpdateHandler {
    void updatePlayerState(PlayerState playerState);
}
