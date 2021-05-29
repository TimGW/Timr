package com.mittylabs.timr.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.google.android.gms.location.*
import com.mittylabs.timr.R
import com.mittylabs.timr.extensions.toast
import com.mittylabs.timr.model.TimerState
import com.mittylabs.timr.notification.Notifications
import com.mittylabs.timr.notification.NotificationsImpl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WalkDetectionService : Service() {

    @Inject
    lateinit var notifications: Notifications

    private lateinit var pendingIntent: PendingIntent
    private lateinit var activityRecognitionClient: ActivityRecognitionClient
    private val request = ActivityTransitionRequest(
        listOf<ActivityTransition>(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )
    )

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        startForeground(
            NotificationsImpl.NOTIFICATION_ID,
            notifications.getOrCreateNotification(0, TimerState.Terminated)
        )

        activityRecognitionClient = ActivityRecognitionClient(this)

        val piFlag = if (Build.VERSION.CODENAME == "S" ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT

        pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(TimerService.TRANSITIONS_RECEIVER_ACTION),
            piFlag // needs to be mutable on android S
        )
        requestActivityUpdates()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        removeActivityUpdates()
    }

    private fun requestActivityUpdates() {
        activityRecognitionClient.requestActivityTransitionUpdates(
            request,
            pendingIntent
        ).addOnSuccessListener {
            toast(getString(R.string.service_walk_detection_register_success))
        }.addOnFailureListener {
            toast(getString(R.string.service_walk_detection_register_fail))
        }
    }

    private fun removeActivityUpdates() {
        activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
            .addOnSuccessListener {
                toast(getString(R.string.service_walk_detection_unregister_success))
            }.addOnFailureListener {
                toast(getString(R.string.service_walk_detection_unregister_fail))
            }
    }
}