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
    const val NOTIFICATION_ID = 2308

    private lateinit var channelIdRunningTimers: String
    private lateinit var channelIdFinishedTimers: String
    private lateinit var pausePendingIntent: PendingIntent
    private lateinit var stopPendingIntent: PendingIntent
    private lateinit var extendPendingIntent: PendingIntent
    private lateinit var terminatePendingIntent: PendingIntent

    private var timerLengthMillis by Delegates.notNull<Long>()

    fun Context.createNotification(timerLength: Long): Notification {
        createChannels(this)

        val pIntent = Intent(this, TimerService::class.java).apply { action = PAUSE_ACTION }
        val sIntent = Intent(this, TimerService::class.java).apply { action = STOP_ACTION }
        val tIntent = Intent(this, TimerService::class.java).apply { action = TERMINATE_ACTION }
        val eIntent = Intent(this, TimerService::class.java).apply { action = EXTEND_ACTION }

        pausePendingIntent = getService(this, REQUEST_CODE, pIntent, FLAG_UPDATE_CURRENT)
        stopPendingIntent = getService(this, REQUEST_CODE, sIntent, FLAG_UPDATE_CURRENT)
        terminatePendingIntent = getService(this, REQUEST_CODE, tIntent, FLAG_UPDATE_CURRENT)
        extendPendingIntent = getService(this, REQUEST_CODE, eIntent, FLAG_UPDATE_CURRENT)

        timerLengthMillis = timerLength

        return playStateNotification(this, timerLength)
    }

    fun Context.updateTimeLeft(remainingTimeMillis: Long) {
        NotificationManagerCompat.from(this)
            .notify(NOTIFICATION_ID, playStateNotification(this, remainingTimeMillis))
    }

    fun Context.updatePauseState(remainingTimeMillis: Long) {
        val notification = baseNotificationBuilder(
            this,
            remainingTimeMillis,
            getString(R.string.notification_state_paused)
        ).apply {
            addAction(
                R.drawable.ic_play_white,
                getString(R.string.notification_action_resume),
                getPlayPendingIntent(this@updatePauseState, true)
            )
            addAction(
                R.drawable.ic_stop_white,
                getString(R.string.notification_action_stop),
                stopPendingIntent
            )
        }.build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    fun Context.updateStopState() {
        val notification = baseNotificationBuilder(
            this,
            timerLengthMillis,
            getString(R.string.notification_state_stopped)
        ).apply {
            addAction(
                R.drawable.ic_play_white,
                getString(R.string.notification_action_start),
                getPlayPendingIntent(this@updateStopState, false)
            )
            addAction(
                R.drawable.ic_clear_white,
                getString(R.string.notification_action_terminate),
                terminatePendingIntent
            )
        }.build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    fun Context.updateFinishedState(elapsedTime: Long) {
        val notification = baseNotificationBuilder(
            this,
            elapsedTime,
            this.getString(R.string.notification_state_finished),
            channelIdFinishedTimers
        ).apply {
            if (VERSION.SDK_INT >= VERSION_CODES.N) priority = NotificationManager.IMPORTANCE_HIGH
            setOnlyAlertOnce(false)
            setDefaults(DEFAULT_LIGHTS)
            setSound(getNotificationSound(this@updateFinishedState))
            setCategory(NotificationCompat.CATEGORY_ALARM)
            addAction(
                R.drawable.ic_clear_white,
                getString(R.string.notification_action_terminate),
                terminatePendingIntent
            )
            addAction(
                R.drawable.ic_add_time_white,
                getString(R.string.notification_action_extend),
                extendPendingIntent
            )
        }.build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    fun Context.removeNotifications() {
        NotificationManagerCompat.from(this).cancelAll()
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
        timeLeft: Long,
        contentPostFix: String = "",
        timerChannel: String = channelIdRunningTimers
    ): NotificationCompat.Builder {
        val intent = Intent(context, TimerActivity::class.java)
            .putExtra(TimerActivity.INTENT_EXTRA_TIMER, TimerService.timerState)

        return NotificationCompat.Builder(context, timerChannel).apply {
            setSmallIcon(R.drawable.ic_timer)
            setContentTitle(timeLeft.toHumanFormat())
            setContentText(context.getString(R.string.notification_content_text, contentPostFix))
            setOnlyAlertOnce(true)
            setCategory(NotificationCompat.CATEGORY_SERVICE)
            color = ContextCompat.getColor(context, R.color.color_primary)
            setContentIntent(getActivity(context, REQUEST_CODE, intent, FLAG_UPDATE_CURRENT))
            setDeleteIntent(terminatePendingIntent).build()
        }
    }

    private fun playStateNotification(
        context: Context,
        remainingTimeMillis: Long
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