package com.shuffler.spotify.listener;

import com.shuffler.activity.MainActivity;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.Capabilities;

public class CapabilitiesCallback implements CallResult.ResultCallback<Capabilities> {

    private MainActivity main;

    public CapabilitiesCallback(MainActivity activity){
        main = activity;
    }

    @Override
    public void onResult(Capabilities capabilities) {
        main.checkPremiumSubscription(capabilities);
    }
}
