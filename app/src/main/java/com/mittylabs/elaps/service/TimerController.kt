package com.mittylabs.elaps.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat.*
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.mittylabs.elaps.R
import com.mittylabs.elaps.service.TimerService.Companion.EXTEND_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.NOTIFICATION_ID
import com.mittylabs.elaps.service.TimerService.Companion.PAUSE_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.START_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.STOP_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.TERMINATE_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.TIMER_LENGTH_EXTRA
import com.mittylabs.elaps.service.TimerService.Companion.TIMER_PAUSED_STATE_EXTRA
import com.mittylabs.elaps.ui.main.TimerState
import com.mittylabs.elaps.ui.toHumanFormat
import kotlin.properties.Delegates

// TODO refactor this class to use broadcast intent's that the activity can subscribe to
object TimerController : Timer {
    private const val REQUEST_CODE = 29
    private var contentPendingIntent: PendingIntent? = null
    private var finishListener: onFinishListener? = null
    private var tickListener: onTickListener? = null
    private var stateChangedListener: onStateChangedListener? = null

    private lateinit var channelId: String
    private lateinit var pausePendingIntent: PendingIntent
    private lateinit var stopPendingIntent: PendingIntent
    private lateinit var extendPendingIntent: PendingIntent
    private lateinit var terminatePendingIntent: PendingIntent

    private var timerLengthMillis by Delegates.notNull<Long>()

    override fun play(context: Context, timeMillis: Long) {
        if (TimerService.timerState == TimerState.RUNNING) return

        Intent(context, TimerService::class.java)
            .apply {
                action = START_ACTION
                putExtra(TIMER_LENGTH_EXTRA, timeMillis)
                putExtra(TIMER_PAUSED_STATE_EXTRA, TimerService.timerState == TimerState.PAUSED)
            }
            .also { startForegroundService(context, it) }
    }

    override fun pause(context: Context) {
        if (TimerService.timerState != TimerState.RUNNING) return

        Intent(context, TimerService::class.java)
            .apply { action = PAUSE_ACTION }
            .also { startForegroundService(context, it) }
    }

    override fun stop(context: Context) {
        if (TimerService.timerState == TimerState.STOPPED) return

        Intent(context, TimerService::class.java)
            .apply { action = STOP_ACTION }
            .also { startForegroundService(context, it) }
    }

    override fun terminate(context: Context) {
        if (TimerService.timerState == TimerState.TERMINATED) return

        Intent(context, TimerService::class.java)
            .apply { action = TERMINATE_ACTION }
            .also { startForegroundService(context, it) }
    }

    fun createNotification(context: Context, timerLength: Long): Notification {
        channelId = "${context.packageName}.timer"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "timer", NotificationManager.IMPORTANCE_DEFAULT).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            from(context).createNotificationChannel(channel)
        }

        val pauseIntent = Intent(context, TimerService::class.java).apply { action = PAUSE_ACTION }
        val stopIntent = Intent(context, TimerService::class.java).apply { action = STOP_ACTION }
        val terminateIntent = Intent(context, TimerService::class.java).apply { action = TERMINATE_ACTION }
        val extendIntent = Intent(context, TimerService::class.java).apply { action = EXTEND_ACTION }

        pausePendingIntent = getService(context, REQUEST_CODE, pauseIntent, FLAG_UPDATE_CURRENT)
        stopPendingIntent = getService(context, REQUEST_CODE, stopIntent, FLAG_UPDATE_CURRENT)
        terminatePendingIntent = getService(context, REQUEST_CODE, terminateIntent, FLAG_UPDATE_CURRENT)
        extendPendingIntent = getService(context, REQUEST_CODE, extendIntent, FLAG_UPDATE_CURRENT)

        timerLengthMillis = timerLength

        return playStateNotification(context, timerLength.toHumanFormat())
    }

    fun updateTimeLeft(context: Context, remainingTimeMillis: String) {
        from(context).notify(NOTIFICATION_ID, playStateNotification(context, remainingTimeMillis))
    }

    fun updatePauseState(context: Context, remainingTimeMillis: String) {
        from(context).notify(NOTIFICATION_ID, pauseStateNotification(context, remainingTimeMillis))
    }

    fun updateStopState(context: Context, timeUp: Boolean = false) {
        from(context).notify(NOTIFICATION_ID, stoppedStateNotification(context))
        if (timeUp) {
//            from(context).cancelAll() todo high important notification with alarm
//            from(context).notify(11, finishedStateNotification(context))
            finishListener?.invoke()
        }
    }

    fun updateUntilFinished(timerLengthMillis: Long, millisUntilFinished: Long) {
        tickListener?.invoke(timerLengthMillis, millisUntilFinished)
    }

    fun removeNotification(context: Context) {
        from(context).cancelAll()
    }

    fun updateTimerState(timerState: TimerState): TimerState {
        stateChangedListener?.invoke(timerState); return timerState
    }

    private fun baseNotificationBuilder(
        context: Context,
        timeLeft: String,
        contentPostFix: String
    ) = NotificationCompat.Builder(context, channelId).apply {
        setSmallIcon(R.drawable.ic_timer)
        setContentTitle(timeLeft)
        setContentText(context.getString(R.string.notification_content_text, contentPostFix))
        setOnlyAlertOnce(true)
        color = ContextCompat.getColor(context, R.color.color_primary)
        contentPendingIntent?.let { setContentIntent(it) }
    }

    private fun playStateNotification(
        context: Context,
        remainingTimeMillis: String
    ) = baseNotificationBuilder(context, remainingTimeMillis, "").apply {
        addAction(R.drawable.ic_pause_white, context.getString(R.string.notification_action_pause), pausePendingIntent)
        addAction(R.drawable.ic_add_time_white, context.getString(R.string.notification_action_extend), extendPendingIntent)
    }.build()

    private fun pauseStateNotification(
        context: Context,
        remainingTimeMillis: String
    ): Notification = baseNotificationBuilder(context, remainingTimeMillis, context.getString(R.string.notification_state_paused)).apply {
        addAction(R.drawable.ic_play_white, context.getString(R.string.notification_action_resume), getPlayPendingIntent(context, true))
        addAction(R.drawable.ic_stop_white, context.getString(R.string.notification_action_stop), stopPendingIntent)
        setDeleteIntent(terminatePendingIntent).build()
    }.build()

    private fun stoppedStateNotification(
        context: Context
    ): Notification = baseNotificationBuilder(context, timerLengthMillis.toHumanFormat(), context.getString(R.string.notification_state_stopped)).apply {
        addAction(R.drawable.ic_play_white, context.getString(R.string.notification_action_start), getPlayPendingIntent(context, false))
        addAction(R.drawable.ic_clear_white, context.getString(R.string.notification_action_terminate), terminatePendingIntent)
        setDeleteIntent(terminatePendingIntent).build()
    }.build()

    private fun finishedStateNotification(
        context: Context
    ): Notification = baseNotificationBuilder(context, timerLengthMillis.toHumanFormat(),
        context.getString(R.string.notification_state_finished)).apply {
        priority = NotificationManager.IMPORTANCE_HIGH
        setCategory(NotificationCompat.CATEGORY_ALARM)
        setAutoCancel(true)
        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        setDeleteIntent(terminatePendingIntent).build()
    }.build()

    private fun getPlayPendingIntent(
        context: Context,
        isPausingState: Boolean
    ) = Intent(context, TimerService::class.java).apply {
        action = START_ACTION
        putExtra(TIMER_LENGTH_EXTRA, timerLengthMillis)
        putExtra(TIMER_PAUSED_STATE_EXTRA, isPausingState)
    }.let { getService(context, REQUEST_CODE, it, FLAG_UPDATE_CURRENT) }

    class Builder(private val context: Context) {

        fun setContentIntent(intent: PendingIntent): Builder {
            contentPendingIntent = intent
            return this
        }

        fun setOnFinishListener(listener: onFinishListener): Builder {
            finishListener = listener
            return this
        }

        fun setOnTickListener(listener: onTickListener): Builder {
            tickListener = listener
            return this
        }

        fun setOnStateChangedListener(listener: onStateChangedListener): Builder {
            stateChangedListener = listener
            return this
        }

        fun play(timeMillis: Long) = play(context, timeMillis)
        fun pause() = pause(context)
        fun stop() = stop(context)
        fun terminate() = terminate(context)
    }
}