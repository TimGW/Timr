package com.mittylabs.elaps.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.location.DetectedActivity
import com.mittylabs.elaps.R
import com.mittylabs.elaps.app.SharedPrefs
import com.mittylabs.elaps.extensions.toasty
import com.mittylabs.elaps.notification.Notifications
import com.mittylabs.elaps.notification.NotificationsImpl.Companion.NOTIFICATION_ID
import com.mittylabs.elaps.timer.TimerActivity.Companion.INTENT_EXTRA_TIMER
import com.mittylabs.elaps.model.TimerState
import com.mittylabs.elaps.timer.TimerActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    companion object {
        private const val TICK_INTERVAL = 50L
        private const val ONE_SECOND = 1000L
        private const val FIVE_MINUTES = 1000 * 60 * 5

        const val START_ACTION = "PLAY"
        const val RESUME_ACTION = "RESUME"
        const val PAUSE_ACTION = "PAUSE"
        const val STOP_ACTION = "STOP"
        const val TERMINATE_ACTION = "TERMINATE"
        const val EXTEND_ACTION = "EXTEND"

        const val TIMER_LENGTH_EXTRA = "timerLengthMilliseconds"

        const val INTENT_EXTRA_SERVICE = "INTENT_EXTRA_SERVICE"
        const val INTENT_EXTRA_ACTIVITY_TYPE = "INTENT_EXTRA_ACTIVITY_TYPE"
    }

    @Inject
    lateinit var notifications: Notifications

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    var timerState: TimerState = TimerState.Terminated
        private set

    private lateinit var timer: CountDownTimer
    private var initialTimerLength: Long = 0L
    private var currentTimerLength: Long = 0L
    private var currentTimeRemaining: Long = 0L
    private var elapsedFinishedTime = 0L
    private val binder = LocalBinder()
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val finishedRunnable: Runnable = object : Runnable {
        override fun run() {
            try {
                elapsedFinishedTime -= ONE_SECOND
                broadcast(TimerState.Finished(true, elapsedFinishedTime))
                notifications.updateFinishedState(elapsedFinishedTime, timerState)
            } finally {
                handler.postDelayed(this, ONE_SECOND)
            }
        }
    }

    private val userActivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == INTENT_EXTRA_SERVICE) {
                when (intent.getIntExtra(INTENT_EXTRA_ACTIVITY_TYPE, -1)) {
                    DetectedActivity.ON_FOOT,
                    DetectedActivity.RUNNING,
                    DetectedActivity.WALKING -> {
                        stopTimer(true)
                        resumeTimer()
                        toasty(getString(R.string.toast_reset_timer))
                    }
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            handler.removeCallbacks(finishedRunnable)

            when (intent.action) {
                START_ACTION -> startTimer(intent.getLongExtra(TIMER_LENGTH_EXTRA, 0L))
                RESUME_ACTION -> resumeTimer()
                PAUSE_ACTION -> pauseTimer()
                STOP_ACTION -> stopTimer(true)
                TERMINATE_ACTION -> terminateTimer()
                EXTEND_ACTION -> extendTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(timerLength: Long) {
        currentTimerLength = timerLength
        currentTimeRemaining = timerLength
        initialTimerLength = timerLength

        val notification = notifications.getOrCreateNotification(timerLength, timerState)
        startForeground(NOTIFICATION_ID, notification)

        if (sharedPrefs.getIsResetEnabled()) {
            registerReceiver(userActivityReceiver, IntentFilter(INTENT_EXTRA_SERVICE))
            ContextCompat.startForegroundService(
                this,
                Intent(this, DetectingActivityService::class.java)
            )
        }

        resumeTimer()
    }

    private fun resumeTimer() {
        if (::timer.isInitialized) timer.cancel()

        timer = createCountDownTimer(currentTimeRemaining)
        timer.start()
        broadcast(TimerState.Started(currentTimerLength, currentTimeRemaining, false))
    }

    private fun pauseTimer() {
        if (::timer.isInitialized) timer.cancel()

        broadcast(TimerState.Paused(currentTimerLength, currentTimeRemaining, true))
        notifications.updatePauseState(currentTimeRemaining, timerState)
    }

    private fun stopTimer(resetTime: Boolean) {
        if (::timer.isInitialized) timer.cancel()
        if (resetTime) {
            currentTimerLength = initialTimerLength
            currentTimeRemaining = initialTimerLength
        }

        broadcast(TimerState.Stopped(currentTimerLength, true))
        notifications.updateStopState(currentTimerLength, timerState)
    }

    private fun terminateTimer() {
        if (::timer.isInitialized) timer.cancel()

        notifications.removeNotifications()
        broadcast(TimerState.Terminated)
        stopSelf()

        if (sharedPrefs.getIsResetEnabled()) {
            stopService(Intent(this, DetectingActivityService::class.java))
            unregisterReceiver(userActivityReceiver)
        }
    }

    private fun extendTimer() {
        currentTimerLength += FIVE_MINUTES
        currentTimeRemaining += FIVE_MINUTES

        when (timerState) {
            is TimerState.Started,
            is TimerState.Finished,
            is TimerState.Progress -> resumeTimer()
            is TimerState.Paused -> pauseTimer()
            is TimerState.Stopped -> stopTimer(false)
            TimerState.Terminated -> terminateTimer()
        }
    }

    private fun broadcast(state: TimerState) {
        timerState = state
        sendBroadcast(Intent(INTENT_EXTRA_TIMER).apply {
            putExtra(INTENT_EXTRA_TIMER, timerState)
        })
    }

    private fun createCountDownTimer(
        millisInFuture: Long
    ) = object : CountDownTimer(millisInFuture, TICK_INTERVAL) {
        var notificationUpdateThreshold = 0L

        override fun onFinish() {
            finishedRunnable.run()
        }

        override fun onTick(millisUntilFinished: Long) {
            notificationUpdateThreshold += currentTimeRemaining - millisUntilFinished
            currentTimeRemaining = millisUntilFinished

            broadcast(TimerState.Progress(currentTimerLength, millisUntilFinished, false))

            // required for smooth progress bar but prevent spamming of notifications
            if (notificationUpdateThreshold >= ONE_SECOND) {
                notifications.updateTimeLeft(currentTimeRemaining, timerState)
                notificationUpdateThreshold = 0L
            }
        }
    }.also { notifications.updateTimeLeft(currentTimeRemaining, timerState) }
}