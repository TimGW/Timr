package com.mittylabs.elaps.service

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.mittylabs.elaps.service.TimerService.Companion.INTENT_EXTRA_ACTIVITY_TYPE

class WalkDetectionJobIntentService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        val result = ActivityRecognitionResult.extractResult(intent)
        val detectedActivities = result?.probableActivities as? List<*>

        detectedActivities?.forEach {
            broadcastActivity(it as DetectedActivity)
        }

        stopSelf()
    }

    private fun broadcastActivity(activity: DetectedActivity) {
        sendBroadcast(Intent(TimerService.INTENT_EXTRA_SERVICE).apply {
            putExtra(INTENT_EXTRA_ACTIVITY_TYPE, activity.type)
        })
    }

    companion object {
        private const val JOB_ID = 1000

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, WalkDetectionJobIntentService::class.java, JOB_ID, work)
        }
    }
}
