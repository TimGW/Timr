package com.mittylabs.elaps.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.android.gms.location.ActivityRecognitionClient
import com.mittylabs.elaps.extensions.toasty
import com.mittylabs.elaps.model.TimerState
import com.mittylabs.elaps.notification.Notifications
import com.mittylabs.elaps.notification.NotificationsImpl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WalkDetectionService : Service() {

    @Inject
    lateinit var notifications: Notifications

    private lateinit var pendingIntent: PendingIntent
    private lateinit var activityRecognitionClient: ActivityRecognitionClient

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        startForeground(
            NotificationsImpl.NOTIFICATION_ID,
            notifications.getOrCreateNotification(0, TimerState.Terminated)
        )

        activityRecognitionClient = ActivityRecognitionClient(this)

        pendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(this, WalkDetectionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        requestActivityUpdatesButtonHandler()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        removeActivityUpdatesButtonHandler()
    }

    private fun requestActivityUpdatesButtonHandler() {
        activityRecognitionClient.requestActivityUpdates(
            DETECTION_INTERVAL_IN_MILLISECONDS,
            pendingIntent
        ).addOnSuccessListener {
            toasty("Successfully requested activity updates")
        }.addOnFailureListener {
            toasty("Requesting activity updates failed to start")
        }
    }

    private fun removeActivityUpdatesButtonHandler() {
        activityRecognitionClient.removeActivityUpdates(pendingIntent)
            .addOnSuccessListener {
                toasty("Removed activity updates successfully!")
            }.addOnFailureListener {
                toasty("Failed to remove activity updates!")
            }
    }

    companion object {
        private const val DETECTION_INTERVAL_IN_MILLISECONDS: Long = 1000
    }
}