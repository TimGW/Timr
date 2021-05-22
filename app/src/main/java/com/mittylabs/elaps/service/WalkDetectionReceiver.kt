package com.mittylabs.elaps.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WalkDetectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        WalkDetectionJobIntentService.enqueueWork(context, intent)
    }
}