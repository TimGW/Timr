package com.mittylabs.elaps.service

import android.app.IntentService
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.mittylabs.elaps.service.TimerService.Companion.INTENT_EXTRA_ACTIVITY_TYPE

class DetectedActivitiesIntentService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        val result = ActivityRecognitionResult.extractResult(intent)
        val detectedActivities = result?.probableActivities as List<*>
        for (activity in detectedActivities) {
            broadcastActivity(activity as DetectedActivity)
        }
    }

    private fun broadcastActivity(activity: DetectedActivity) {
        sendBroadcast(Intent(TimerService.INTENT_EXTRA_SERVICE).apply {
            putExtra(INTENT_EXTRA_ACTIVITY_TYPE, activity.type)
        })
    }

    companion object {
        private val TAG = DetectedActivitiesIntentService::class.java.simpleName
    }
}