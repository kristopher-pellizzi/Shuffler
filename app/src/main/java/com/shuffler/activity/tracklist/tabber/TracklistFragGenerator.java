package com.shuffler.activity.tracklist.tabber;

import androidx.fragment.app.Fragment;
import com.shuffler.activity.tracklist.tabber.fragment.AllTracksFragment;
import com.shuffler.activity.tracklist.tabber.fragment.EnqueuedFragment;
import com.shuffler.activity.tracklist.tabber.fragment.ToBeEnqueuedFragment;


public class TracklistFragGenerator {

    public static Fragment newInstance(int index){
        Fragment fragment;

        switch(index){
            case 0:
                fragment = EnqueuedFragment.getInstance();
                break;
            case 1:
                fragment = ToBeEnqueuedFragment.getInstance();
                break;
            case 2:
                fragment = AllTracksFragment.getInstance();
                break;
            default:
                fragment = null;
        }

        return fragment;
    }
}
