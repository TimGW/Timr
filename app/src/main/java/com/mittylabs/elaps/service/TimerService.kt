package com.mittylabs.elaps.service

import android.app.Service
import android.content.Intent
import android.os.*
import com.mittylabs.elaps.service.NotificationController.NOTIFICATION_ID
import com.mittylabs.elaps.service.NotificationController.createNotification
import com.mittylabs.elaps.service.NotificationController.removeNotifications
import com.mittylabs.elaps.service.NotificationController.updateFinishedState
import com.mittylabs.elaps.service.NotificationController.updatePauseState
import com.mittylabs.elaps.service.NotificationController.updateStopState
import com.mittylabs.elaps.service.NotificationController.updateTimeLeft
import com.mittylabs.elaps.ui.main.TimerActivity.Companion.INTENT_EXTRA_TIMER
import com.mittylabs.elaps.ui.main.TimerState

class TimerService : Service() {

    companion object {
        private const val TICK_INTERVAL = 1000L
        private const val FIVE_MINUTES_IN_MILLIS = 1000 * 60 * 5

        const val START_ACTION = "PLAY"
        const val PAUSE_ACTION = "PAUSE"
        const val STOP_ACTION = "STOP"
        const val TERMINATE_ACTION = "TERMINATE"
        const val EXTEND_ACTION = "EXTEND"

        const val TIMER_LENGTH_EXTRA = "timerLengthMilliseconds"
        const val TIMER_PAUSED_STATE_EXTRA = "timerIsPausingState"

        var timerState: TimerState = TimerState.Terminated(0L, 0L)
    }

    private val binder = LocalBinder()
    private lateinit var timer: CountDownTimer
    private var initialTimerLength: Long = 0L
    private var currentTimerLength: Long = 0L
    private var currentTimeRemaining: Long = 0L
    private var timeElapsed = 0L
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val finishedRunnable: Runnable = object : Runnable {
        override fun run() {
            try {
                timeElapsed -= TICK_INTERVAL
                broadcast(
                    TimerState.Finished(
                        currentTimerLength,
                        currentTimeRemaining,
                        timeElapsed
                    )
                )
                updateFinishedState(timeElapsed)
            } finally {
                handler.postDelayed(this, TICK_INTERVAL)
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
                START_ACTION -> playTimer(
                    intent.getLongExtra(TIMER_LENGTH_EXTRA, 0L),
                    intent.getBooleanExtra(TIMER_PAUSED_STATE_EXTRA, false)
                )
                PAUSE_ACTION -> pauseTimer()
                STOP_ACTION -> stopTimer()
                TERMINATE_ACTION -> terminateTimer()
                EXTEND_ACTION -> extendTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun playTimer(timerLength: Long, isPausingState: Boolean) {
        if (!isPausingState) {
            currentTimerLength = timerLength
            currentTimeRemaining = timerLength
            initialTimerLength = timerLength
        }
        startForeground(NOTIFICATION_ID, createNotification(timerLength))
        timer = createCountDownTimer(currentTimeRemaining).start()
        broadcast(TimerState.Running(timerLength, currentTimeRemaining))
    }

    private fun pauseTimer() {
        if (::timer.isInitialized) timer.cancel()
        broadcast(TimerState.Paused(currentTimerLength, currentTimeRemaining))
        updatePauseState(currentTimerLength, currentTimeRemaining)
    }

    private fun stopTimer() {
        if (::timer.isInitialized) timer.cancel()
        broadcast(TimerState.Stopped(initialTimerLength, currentTimeRemaining))
        updateStopState(initialTimerLength)
    }

    private fun terminateTimer() {
        if (::timer.isInitialized) timer.cancel()
        broadcast(TimerState.Terminated(initialTimerLength, currentTimeRemaining))
        removeNotifications()
        stopSelf()
    }

    private fun extendTimer() {
        if (::timer.isInitialized) {
            currentTimerLength += FIVE_MINUTES_IN_MILLIS
            currentTimeRemaining += FIVE_MINUTES_IN_MILLIS

            timer.cancel()
            timer = createCountDownTimer(currentTimeRemaining)
            timer.start()
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

        override fun onFinish() {
            finishedRunnable.run()
        }

        override fun onTick(millisUntilFinished: Long) {
            currentTimeRemaining = millisUntilFinished
            broadcast(TimerState.Running(currentTimerLength, millisUntilFinished))
            updateTimeLeft(currentTimeRemaining)
        }
    }
}