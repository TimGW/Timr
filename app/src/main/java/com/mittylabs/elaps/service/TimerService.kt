package com.mittylabs.elaps.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.mittylabs.elaps.BuildConfig
import com.mittylabs.elaps.R
import com.mittylabs.elaps.app.ElapsApp
import com.mittylabs.elaps.app.SharedPrefs
import com.mittylabs.elaps.extensions.nanoToSeconds
import com.mittylabs.elaps.extensions.toast
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
            BuildConfig.APPLICATION_ID + ".TRANSITIONS_RECEIVER_ACTION"

        var timerState: TimerState = TimerState.Terminated
            private set
    }

    @Inject lateinit var notifications: Notifications
    @Inject lateinit var sharedPrefs: SharedPrefs
    private lateinit var walkDetectionService: Intent

    private var timer: CountDownTimer? = null
    private var initialTimerLength: Long = 0L
    private var currentTimerLength: Long = 0L
    private var currentTimeRemaining: Long = 0L
    private var elapsedFinishedTime = 0L

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val finishedRunnable: Runnable = object : Runnable {
        override fun run() {
            try {
                elapsedFinishedTime -= ONE_SECOND
                broadcastState(TimerState.Finished(elapsedFinishedTime))
                notifications.updateFinishedState(elapsedFinishedTime, timerState)
            } finally {
                handler.postDelayed(this, ONE_SECOND)
            }
        }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TRANSITIONS_RECEIVER_ACTION) {
                if (ActivityTransitionResult.hasResult(intent)) {
                    ActivityTransitionResult.extractResult(intent)?.transitionEvents?.forEach {
                        if (it.elapsedRealTimeNanos.nanoToSeconds() <= THIRTY_SECONDS) {
                            val activityType = it.activityType
                            val transitionType = it.transitionType
                            handleTransitionResult(activityType, transitionType)
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        Log.d(ElapsApp.TAG, "service, onCreate")

        // don't use LocalBroadcastManager otherwise the intent's won't be received
        registerReceiver(receiver, IntentFilter().apply {
            addAction(TRANSITIONS_RECEIVER_ACTION)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        super.onStartCommand(intent, flags, startId)
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
            Log.d(ElapsApp.TAG, "service, onStartCommand: ${intent.action}")
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        Log.d(ElapsApp.TAG, "service, onDestroy")
    }

    private fun startTimer(timerLength: Long) {
        currentTimerLength = timerLength
        currentTimeRemaining = timerLength
        initialTimerLength = timerLength

        val notification = notifications.getOrCreateNotification(timerLength, timerState)
        startForeground(NOTIFICATION_ID, notification)

        resumeTimer()

        if (sharedPrefs.isResetEnabled()) {
            walkDetectionService = Intent(this, WalkDetectionService::class.java)
            ContextCompat.startForegroundService(this, walkDetectionService)
        }
    }

    private fun resumeTimer() {
        timer?.cancel()
        timer = createCountDownTimer(currentTimeRemaining).also { it.start() }
        broadcastState(TimerState.Started(currentTimerLength, currentTimeRemaining))
    }

    private fun pauseTimer() {
        timer?.cancel()
        broadcastState(TimerState.Paused(currentTimerLength, currentTimeRemaining))
        notifications.updatePauseState(currentTimeRemaining, timerState)
    }

    private fun stopTimer(resetTime: Boolean) {
        timer?.cancel()
        if (resetTime) {
            currentTimerLength = initialTimerLength
            currentTimeRemaining = initialTimerLength
        }

        broadcastState(TimerState.Stopped(currentTimerLength))
        notifications.updateStopState(currentTimerLength, timerState)
    }

    private fun terminateTimer() {
        timer?.cancel()

        if (sharedPrefs.isResetEnabled()) stopService(walkDetectionService)
        broadcastState(TimerState.Terminated)
        notifications.removeNotifications()
        stopSelf()
    }

    private fun extendTimer() {
        currentTimerLength += FIVE_MINUTES
        currentTimeRemaining += FIVE_MINUTES

        when (timerState) {
            is TimerState.Started,
            is TimerState.Finished -> resumeTimer()
            is TimerState.Paused -> pauseTimer()
            is TimerState.Stopped -> stopTimer(false)
            TimerState.Terminated -> terminateTimer()
        }
    }

    private fun broadcastState(state: TimerState) {
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

            broadcastState(TimerState.Started(currentTimerLength, millisUntilFinished))

            // required for smooth progress bar but prevent spamming of notifications
            if (notificationUpdateThreshold >= ONE_SECOND) {
                notifications.updateTimeLeft(currentTimeRemaining, timerState)
                notificationUpdateThreshold = 0L
            }
        }
    }.also { notifications.updateTimeLeft(currentTimeRemaining, timerState) }

    private fun handleTransitionResult(
        activityType: Int,
        transitionType: Int = ActivityTransition.ACTIVITY_TRANSITION_ENTER
    ) {
        if (activityType == DetectedActivity.STILL &&
            transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        ) {
            if (timerState is TimerState.Started) return // timer already running

            resumeTimer()
            toast(getString(R.string.toast_reset_timer_resume))
        } else if (activityType == DetectedActivity.STILL &&
            transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT
        ) {
            // is timer already stopped?
            if (timerState is TimerState.Stopped) return

            stopTimer(true)
            toast(getString(R.string.toast_reset_timer))
        }
    }
}