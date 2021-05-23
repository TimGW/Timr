package com.mittylabs.elaps.worker//package com.mittylabs.elaps.service
//
//import android.content.Context
//import android.content.Intent
//import androidx.hilt.work.HiltWorker
//import androidx.work.Worker
//import androidx.work.WorkerParameters
//import com.google.android.gms.location.ActivityRecognitionResult
//import com.google.android.gms.location.DetectedActivity
//import com.mittylabs.elaps.notification.Notifications
//import dagger.assisted.Assisted
//import dagger.assisted.AssistedInject
//
//@HiltWorker
//class DetectedActivityWorker @AssistedInject constructor(
//    @Assisted context: Context,
//    @Assisted params: WorkerParameters,
//    private val notifications: Notifications
//) : Worker(context, params) {
//
//    override fun doWork() = try {
////        val title = inputData.getString(NotificationQueueImpl.NOTIFICATION_TITLE_KEY)
////            ?: context.getString(R.string.app_name)
////        val text = inputData.getString(NotificationQueueImpl.NOTIFICATION_MSG_KEY)
////            ?: context.getString(R.string.notification_default_msg)
////        val id = tags.first().toString()
//
////        notifications.notify(
////            id,
////            context.getString(R.string.notification_title, title),
////            context.getString(R.string.notification_message, text)
////        )
//        val result = ActivityRecognitionResult.extractResult(intent)
//        val detectedActivities = result?.probableActivities as List<*>
//        for (activity in detectedActivities) {
//            broadcastActivity(activity as DetectedActivity)
//        }
//        Result.success()
//    } catch (e: Exception) {
//        Result.failure()
//    }
//
//    private fun broadcastActivity(activity: DetectedActivity) {
//        sendBroadcast(Intent(TimerService.INTENT_EXTRA_SERVICE).apply {
//            putExtra(TimerService.INTENT_EXTRA_ACTIVITY_TYPE, activity.type)
//        })
//    }
//}