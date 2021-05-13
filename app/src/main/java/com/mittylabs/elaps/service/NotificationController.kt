package com.mittylabs.elaps.service

import android.app.Notification
import android.app.Notification.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mittylabs.elaps.R
import com.mittylabs.elaps.service.TimerService.Companion.EXTEND_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.NOTIFICATION_ID
import com.mittylabs.elaps.service.TimerService.Companion.PAUSE_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.START_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.STOP_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.TERMINATE_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.TIMER_LENGTH_EXTRA
import com.mittylabs.elaps.service.TimerService.Companion.TIMER_PAUSED_STATE_EXTRA
import com.mittylabs.elaps.ui.main.TimerActivity
import com.mittylabs.elaps.utils.toHumanFormat
import kotlin.properties.Delegates


object NotificationController {
    private const val REQUEST_CODE = 29
    private var contentPendingIntent: PendingIntent? = null

    private lateinit var channelIdRunningTimers: String
    private lateinit var channelIdFinishedTimers: String
    private lateinit var pausePendingIntent: PendingIntent
    private lateinit var stopPendingIntent: PendingIntent
    private lateinit var extendPendingIntent: PendingIntent
    private lateinit var terminatePendingIntent: PendingIntent

    private var timerLengthMillis by Delegates.notNull<Long>()

    fun createNotification(context: Context, timerLength: Long): Notification {
        createChannels(context)

        val pIntent = Intent(context, TimerService::class.java).apply { action = PAUSE_ACTION }
        val sIntent = Intent(context, TimerService::class.java).apply { action = STOP_ACTION }
        val tIntent = Intent(context, TimerService::class.java).apply { action = TERMINATE_ACTION }
        val eIntent = Intent(context, TimerService::class.java).apply { action = EXTEND_ACTION }

        pausePendingIntent = getService(context, REQUEST_CODE, pIntent, FLAG_UPDATE_CURRENT)
        stopPendingIntent = getService(context, REQUEST_CODE, sIntent, FLAG_UPDATE_CURRENT)
        terminatePendingIntent = getService(context, REQUEST_CODE, tIntent, FLAG_UPDATE_CURRENT)
        extendPendingIntent = getService(context, REQUEST_CODE, eIntent, FLAG_UPDATE_CURRENT)

        timerLengthMillis = timerLength

        return playStateNotification(context, timerLength.toHumanFormat())
    }

    fun updateTimeLeft(context: Context, remainingTimeMillis: String) {
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, playStateNotification(context, remainingTimeMillis))
    }

    fun updatePauseState(context: Context, remainingTimeMillis: String) {
        val notification = baseNotificationBuilder(
            context,
            remainingTimeMillis,
            context.getString(R.string.notification_state_paused)
        ).apply {
            addAction(
                R.drawable.ic_play_white,
                context.getString(R.string.notification_action_resume),
                getPlayPendingIntent(context, true)
            )
            addAction(
                R.drawable.ic_stop_white,
                context.getString(R.string.notification_action_stop),
                stopPendingIntent
            )
        }.build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun updateStopState(context: Context) {
        val notification = baseNotificationBuilder(
            context,
            timerLengthMillis.toHumanFormat(),
            context.getString(R.string.notification_state_stopped)
        ).apply {
            addAction(
                R.drawable.ic_play_white,
                context.getString(R.string.notification_action_start),
                getPlayPendingIntent(context, false)
            )
            addAction(
                R.drawable.ic_clear_white,
                context.getString(R.string.notification_action_terminate),
                terminatePendingIntent
            )
        }.build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun updateFinishedState(context: Context, elapsedTime: Long) {
        val notification = baseNotificationBuilder(
            context,
            "- ${elapsedTime.toHumanFormat()}",
            context.getString(R.string.notification_state_finished),
            channelIdFinishedTimers
        ).apply {
            if (VERSION.SDK_INT >= VERSION_CODES.N) priority = NotificationManager.IMPORTANCE_HIGH
            setOnlyAlertOnce(false)
            setDefaults(DEFAULT_LIGHTS)
            setSound(getNotificationSound(context))
            setCategory(NotificationCompat.CATEGORY_ALARM)
            addAction(
                R.drawable.ic_clear_white,
                context.getString(R.string.notification_action_terminate),
                terminatePendingIntent
            )
            addAction(
                R.drawable.ic_add_time_white,
                context.getString(R.string.notification_action_extend),
                extendPendingIntent
            )
        }.build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun removeNotification(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun getNotificationSound(context: Context) = Uri.parse(
        "android.resource://" +
                context.packageName +
                "/" +
                R.raw.alarm_sound
    )

    private fun createChannels(context: Context) {
        channelIdRunningTimers = "${context.packageName}.timer.running"
        channelIdFinishedTimers = "${context.packageName}.timer.finished"

        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(CONTENT_TYPE_SONIFICATION)
                .build()

            val runningTimerChannel = NotificationChannel(
                channelIdRunningTimers,
                "Timers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_timer_running_desc)
                lockscreenVisibility = VISIBILITY_PUBLIC
            }

            val finishedTimerChannel = NotificationChannel(
                channelIdFinishedTimers,
                "Firing timers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_timer_finished_desc)
                lockscreenVisibility = VISIBILITY_PUBLIC
                setBypassDnd(true)
                setSound(getNotificationSound(context), attributes)
                enableLights(true)
            }

            NotificationManagerCompat.from(context).createNotificationChannels(
                listOf(runningTimerChannel, finishedTimerChannel)
            )
        }
    }

    private fun baseNotificationBuilder(
        context: Context,
        timeLeft: String,
        contentPostFix: String = "",
        timerChannel: String = channelIdRunningTimers
    ) = NotificationCompat.Builder(context, timerChannel).apply {
        setSmallIcon(R.drawable.ic_timer)
        setContentTitle(timeLeft)
        setContentText(context.getString(R.string.notification_content_text, contentPostFix))
        setOnlyAlertOnce(true)
        setCategory(NotificationCompat.CATEGORY_SERVICE)
        color = ContextCompat.getColor(context, R.color.color_primary)
//        contentPendingIntent?.let { setContentIntent(it) } // FIXME
        setContentIntent(Intent(context, TimerActivity::class.java).let { intent ->
            PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT)
        })
        setDeleteIntent(terminatePendingIntent).build()
    }

    private fun playStateNotification(
        context: Context,
        remainingTimeMillis: String
    ) = baseNotificationBuilder(context, remainingTimeMillis).apply {
        addAction(
            R.drawable.ic_pause_white,
            context.getString(R.string.notification_action_pause),
            pausePendingIntent
        )
        addAction(
            R.drawable.ic_add_time_white,
            context.getString(R.string.notification_action_extend),
            extendPendingIntent
        )
    }.build()

    private fun getPlayPendingIntent(
        context: Context,
        isPausingState: Boolean
    ) = Intent(context, TimerService::class.java).apply {
        action = START_ACTION
        putExtra(TIMER_LENGTH_EXTRA, timerLengthMillis)
        putExtra(TIMER_PAUSED_STATE_EXTRA, isPausingState)
    }.let { getService(context, REQUEST_CODE, it, FLAG_UPDATE_CURRENT) }
}