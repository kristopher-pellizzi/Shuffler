package com.shuffler.utility;

import android.view.View;

import com.shuffler.service.EnqueueingService;

public class ForceEnqueueBtnListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {
        EnqueueingService.worker.enqueue();
    }
}
