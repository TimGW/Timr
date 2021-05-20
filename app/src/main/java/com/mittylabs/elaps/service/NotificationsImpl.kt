package com.mittylabs.elaps.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mittylabs.elaps.R
import com.mittylabs.elaps.ui.timer.TimerActivity
import com.mittylabs.elaps.ui.timer.TimerState
import com.mittylabs.elaps.utils.toHumanFormat

class NotificationsImpl(
    private val context: Context
) : Notifications {
    private lateinit var channelIdRunningTimers: String
    private lateinit var channelIdFinishedTimers: String

    private lateinit var pausePendingIntent: PendingIntent
    private lateinit var resumePendingIntent: PendingIntent
    private lateinit var stopPendingIntent: PendingIntent
    private lateinit var extendPendingIntent: PendingIntent
    private lateinit var terminatePendingIntent: PendingIntent

    companion object {
        private const val REQUEST_CODE = 29
        const val NOTIFICATION_ID = 2308
    }

    override fun createNotification(timerLength: Long, timerState: TimerState): Notification {
        createChannels()

        val pIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.PAUSE_ACTION
        }
        val rIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.RESUME_ACTION
        }
        val sIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.STOP_ACTION
        }
        val tIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.TERMINATE_ACTION
        }
        val eIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.EXTEND_ACTION
        }

        pausePendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE,
            pIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        resumePendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE,
            rIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        stopPendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE,
            sIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        terminatePendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE,
            tIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        extendPendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE,
            eIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        return playStateNotification(timerLength, timerState)
    }

    override fun updateTimeLeft(remainingTimeMillis: Long, timerState: TimerState) {
        NotificationManagerCompat.from(context)
            .notify(
                NOTIFICATION_ID,
                playStateNotification(
                    remainingTimeMillis,
                    timerState
                )
            )
    }

    override fun updatePauseState(currentTimeRemaining: Long, timerState: TimerState) {
        val notification = baseNotificationBuilder(
            timerState,
            currentTimeRemaining,
            context.getString(R.string.notification_state_paused),
        ).apply {
            addAction(
                R.drawable.ic_play_white,
                context.getString(R.string.notification_action_resume),
                resumePendingIntent
            )
            addAction(
                R.drawable.ic_stop_white,
                context.getString(R.string.notification_action_stop),
                stopPendingIntent
            )
        }.build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, notification)
    }

    override fun updateStopState(initialTimerLength: Long, timerState: TimerState) {
        val playPendingIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.START_ACTION
            putExtra(TimerService.TIMER_LENGTH_EXTRA, initialTimerLength)
        }.let {
            PendingIntent.getService(
                context,
                REQUEST_CODE,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = baseNotificationBuilder(
            timerState,
            initialTimerLength,
            context.getString(R.string.notification_state_stopped)
        ).apply {
            addAction(
                R.drawable.ic_play_white,
                context.getString(R.string.notification_action_start),
                playPendingIntent
            )
            addAction(
                R.drawable.ic_clear_white,
                context.getString(R.string.notification_action_terminate),
                terminatePendingIntent
            )
        }.build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, notification)
    }

    override fun updateFinishedState(elapsedTime: Long, timerState: TimerState) {
        val notification = baseNotificationBuilder(
            timerState,
            elapsedTime,
            context.getString(R.string.notification_state_finished),
            channelIdFinishedTimers
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) priority =
                NotificationManager.IMPORTANCE_HIGH
            setOnlyAlertOnce(false)
            setDefaults(Notification.DEFAULT_LIGHTS)
            setSound(getNotificationSound())
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

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, notification)
    }

    override fun removeNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun getNotificationSound() = Uri.parse(
        "android.resource://" +
                context.packageName +
                "/" +
                R.raw.alarm_sound
    )

    private fun createChannels() {
        channelIdRunningTimers = "${context.packageName}.timer.running"
        channelIdFinishedTimers = "${context.packageName}.timer.finished"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val runningTimerChannel = NotificationChannel(
                channelIdRunningTimers,
                "Timers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_timer_running_desc)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val finishedTimerChannel = NotificationChannel(
                channelIdFinishedTimers,
                "Firing timers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_timer_finished_desc)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setSound(getNotificationSound(), attributes)
                enableLights(true)
            }

            NotificationManagerCompat.from(context).createNotificationChannels(
                listOf(runningTimerChannel, finishedTimerChannel)
            )
        }
    }

    private fun baseNotificationBuilder(
        timerState: TimerState,
        timeLeft: Long,
        contentPostFix: String = "",
        timerChannel: String = channelIdRunningTimers,
    ): NotificationCompat.Builder {
        val intent = Intent(context, TimerActivity::class.java)
            .putExtra(TimerActivity.INTENT_EXTRA_TIMER, timerState)

        return NotificationCompat.Builder(context, timerChannel).apply {
            setSmallIcon(R.drawable.ic_timer)
            setContentTitle(timeLeft.toHumanFormat())
            setContentText(context.getString(R.string.notification_content_text, contentPostFix))
            setOnlyAlertOnce(true)
            setCategory(NotificationCompat.CATEGORY_SERVICE)
            color = ContextCompat.getColor(context, R.color.color_primary)
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            setDeleteIntent(terminatePendingIntent).build()
        }
    }

    private fun playStateNotification(
        remainingTimeMillis: Long,
        timerState: TimerState
    ) = baseNotificationBuilder(timerState, remainingTimeMillis).apply {
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
}