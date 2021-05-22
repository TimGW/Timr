package com.mittylabs.elaps.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import com.mittylabs.elaps.R
import com.mittylabs.elaps.extensions.toHumanFormat
import com.mittylabs.elaps.model.TimerState
import com.mittylabs.elaps.service.TimerService
import com.mittylabs.elaps.timer.TimerActivity
import com.mittylabs.elaps.timer.TimerActivity.Companion.INTENT_EXTRA_TIMER
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationsImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : Notifications {
    private val channelIdRunningTimers = "${context.packageName}.timer.running"
    private val channelIdFinishedTimers = "${context.packageName}.timer.finished"
    private var notification: Notification? = null

    companion object {
        private const val REQUEST_CODE = 29
        const val NOTIFICATION_ID = 2308
    }

    override fun getOrCreateNotification(timerLength: Long, timerState: TimerState): Notification {
        if (notification == null) {
            createChannels()
            notification = playStateNotification(timerLength, timerState)
        }
        return notification as Notification
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
        val resumePendingIntent = PendingIntent.getService(
            context, REQUEST_CODE,
            Intent(context, TimerService::class.java).apply {
                action = TimerService.RESUME_ACTION
            }, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopPendingIntent = PendingIntent.getService(
            context, REQUEST_CODE,
            Intent(context, TimerService::class.java).apply {
                action = TimerService.STOP_ACTION
            }, PendingIntent.FLAG_UPDATE_CURRENT
        )

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

        val terminatePendingIntent = PendingIntent.getService(
            context, REQUEST_CODE,
            Intent(context, TimerService::class.java).apply {
                action = TimerService.TERMINATE_ACTION
            }, PendingIntent.FLAG_UPDATE_CURRENT
        )

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
        val extendPendingIntent = PendingIntent.getService(
            context, REQUEST_CODE,
            Intent(context, TimerService::class.java).apply {
                action = TimerService.EXTEND_ACTION
            }, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val terminatePendingIntent = PendingIntent.getService(
            context, REQUEST_CODE,
            Intent(context, TimerService::class.java).apply {
                action = TimerService.TERMINATE_ACTION
            }, PendingIntent.FLAG_UPDATE_CURRENT
        )

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
        val pendingIntent = NavDeepLinkBuilder(context)
            .setComponentName(TimerActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.timerRunningFragment)
            .setArguments(Bundle().apply {
                putParcelable(INTENT_EXTRA_TIMER, timerState)
            })
            .createPendingIntent()

        return NotificationCompat.Builder(context, timerChannel).apply {
            setSmallIcon(R.drawable.ic_timer)
            setContentTitle(timeLeft.toHumanFormat())
            setContentText(context.getString(R.string.notification_content_text, contentPostFix))
            setOnlyAlertOnce(true)
            setCategory(NotificationCompat.CATEGORY_SERVICE)
            color = ContextCompat.getColor(context, R.color.color_primary)
            setContentIntent(pendingIntent)
        }
    }

    private fun playStateNotification(
        remainingTimeMillis: Long,
        timerState: TimerState
    ): Notification {
        val pausePendingIntent = PendingIntent.getService(
            context, REQUEST_CODE,
            Intent(context, TimerService::class.java).apply {
                action = TimerService.PAUSE_ACTION
            }, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val extendPendingIntent = PendingIntent.getService(
            context, REQUEST_CODE,
            Intent(context, TimerService::class.java).apply {
                action = TimerService.EXTEND_ACTION
            }, PendingIntent.FLAG_UPDATE_CURRENT
        )

        return baseNotificationBuilder(timerState, remainingTimeMillis).apply {
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
}