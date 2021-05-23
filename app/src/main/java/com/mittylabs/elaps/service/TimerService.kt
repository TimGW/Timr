package com.mittylabs.elaps.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.mittylabs.elaps.BuildConfig
import com.mittylabs.elaps.R
import com.mittylabs.elaps.app.SharedPrefs
import com.mittylabs.elaps.extensions.toSeconds
import com.mittylabs.elaps.extensions.toasty
import com.mittylabs.elaps.model.TimerState
import com.mittylabs.elaps.notification.Notifications
import com.mittylabs.elaps.notification.NotificationsImpl.Companion.NOTIFICATION_ID
import com.mittylabs.elaps.timer.TimerActivity.Companion.INTENT_EXTRA_TIMER
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    companion object {
        private const val TICK_INTERVAL = 50L
        private const val ONE_SECOND = 1000L
        private const val FIVE_MINUTES = 1000 * 60 * 5
        private const val THIRTY_SECONDS = 1000L * 30L

        const val START_ACTION = "PLAY"
        const val RESUME_ACTION = "RESUME"
        const val PAUSE_ACTION = "PAUSE"
        const val STOP_ACTION = "STOP"
        const val TERMINATE_ACTION = "TERMINATE"
        const val EXTEND_ACTION = "EXTEND"

        const val TIMER_LENGTH_EXTRA = "timerLengthMilliseconds"
        const val TRANSITIONS_RECEIVER_ACTION: String =
            BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION"
    }

    @Inject
    lateinit var notifications: Notifications

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    var timerState: TimerState = TimerState.Terminated
        private set

    private lateinit var timer: CountDownTimer
    private lateinit var walkDetectionService: Intent

    private var initialTimerLength: Long = 0L
    private var currentTimerLength: Long = 0L
    private var currentTimeRemaining: Long = 0L
    private var elapsedFinishedTime = 0L

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val binder = LocalBinder()
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
            if (
                ActivityTransitionResult.hasResult(intent) &&
                intent.action == TRANSITIONS_RECEIVER_ACTION
            ) {
                ActivityTransitionResult.extractResult(intent)?.transitionEvents?.forEach {
                    if (it.elapsedRealTimeNanos.toSeconds() <= THIRTY_SECONDS) {
                        val activityType = it.activityType
                        val transitionType = it.transitionType
                        handleTransitionResult(activityType, transitionType)
                    }
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        // don't use LocalBroadcastManager otherwise the intent's won't be received
        registerReceiver(userActivityReceiver, IntentFilter(TRANSITIONS_RECEIVER_ACTION))
    }

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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(userActivityReceiver)
    }

    private fun startTimer(timerLength: Long) {
        currentTimerLength = timerLength
        currentTimeRemaining = timerLength
        initialTimerLength = timerLength

        val notification = notifications.getOrCreateNotification(timerLength, timerState)
        startForeground(NOTIFICATION_ID, notification)

        if (sharedPrefs.getIsResetEnabled()) {
            walkDetectionService = Intent(this, WalkDetectionService::class.java)
            ContextCompat.startForegroundService(this, walkDetectionService)
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

        if (sharedPrefs.getIsResetEnabled()) stopService(walkDetectionService)
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
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent(INTENT_EXTRA_TIMER).apply {
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

    private fun handleTransitionResult(activityType: Int, transitionType: Int) {
        if (activityType == DetectedActivity.STILL &&
            transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        ) {
            // is timer already running?
            if (timerState is TimerState.Progress ||
                timerState is TimerState.Started
            ) return

            resumeTimer()
            toasty(getString(R.string.toast_reset_timer_resume))
        } else if (activityType == DetectedActivity.WALKING &&
            transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        ) {
            // is timer already stopped?
            if (timerState is TimerState.Stopped) return

            stopTimer(true)
            toasty(getString(R.string.toast_reset_timer))
        }
    }
}