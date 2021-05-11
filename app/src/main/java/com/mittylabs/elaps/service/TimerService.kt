package com.mittylabs.elaps.service

import android.app.Service
import android.content.Intent
import android.os.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mittylabs.elaps.ui.main.TimerState
import com.mittylabs.elaps.utils.toHumanFormat

class TimerService : Service() {

    companion object {
        private const val TICK_INTERVAL = 1000L
        private const val FIVE_MINUTES_IN_MILLIS = 1000 * 60 * 5
        const val NOTIFICATION_ID = 2308

        // todo extract action for RESUME
        const val START_ACTION = "PLAY"
        const val PAUSE_ACTION = "PAUSE"
        const val STOP_ACTION = "STOP"
        const val TERMINATE_ACTION = "TERMINATE"
        const val EXTEND_ACTION = "EXTEND"

        const val TIMER_LENGTH_EXTRA = "timerLengthMilliseconds"
        const val TIMER_PAUSED_STATE_EXTRA = "timerIsPausingState"

        var timerState = TimerState.TERMINATED
    }

    private lateinit var timer: CountDownTimer
    private var timerLengthMillis: Long = 0L
    private var timerRemainingMillis: Long = 0L
    private var elapsedTime = 0L
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var finishedRunnable: Runnable = object : Runnable {
        override fun run() {
            try {
                elapsedTime += TICK_INTERVAL
                NotificationController.updateFinishedState(this@TimerService, elapsedTime)
            } finally {
                handler.postDelayed(this, TICK_INTERVAL)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
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
            timerLengthMillis = timerLength
            timerRemainingMillis = timerLength
        }
        startForeground(NOTIFICATION_ID, NotificationController.createNotification(this, timerLength))

        timer = createCountDownTimer(timerRemainingMillis).start()
        timerState = TimerController.updateTimerState(TimerState.RUNNING)
    }

    private fun pauseTimer() {
        if (::timer.isInitialized) timer.cancel()
        timerState = TimerController.updateTimerState(TimerState.PAUSED)
        NotificationController.updatePauseState(this, timerRemainingMillis.toHumanFormat())
//      fixme stopForeground(false) this will kill the service by the system after ~2 min
    }

    private fun stopTimer() {
        if (::timer.isInitialized) timer.cancel()
        timerState = TimerController.updateTimerState(TimerState.STOPPED)
        NotificationController.updateStopState(this@TimerService)
    }

    private fun terminateTimer() {
        if (::timer.isInitialized) timer.cancel()
        timerState = TimerController.updateTimerState(TimerState.TERMINATED)
        NotificationController.removeNotification(this) // todo broadcast resetUI ?
        handler.removeCallbacks(finishedRunnable)
        stopSelf()
    }

    private fun extendTimer() {
        if (::timer.isInitialized) {
            timerLengthMillis += FIVE_MINUTES_IN_MILLIS
            timerRemainingMillis += FIVE_MINUTES_IN_MILLIS

            timer.cancel()
            handler.removeCallbacks(finishedRunnable)
            timer = createCountDownTimer(timerRemainingMillis).start()

            timerState = TimerController.updateTimerState(TimerState.RUNNING)
        }
    }

    private fun createCountDownTimer(millisInFuture: Long) =
        object : CountDownTimer(millisInFuture, TICK_INTERVAL) {

            override fun onFinish() {
                timerState = TimerController.updateTimerState(TimerState.STOPPED)
                finishedRunnable.run()
            }

            override fun onTick(millisUntilFinished: Long) {
                timerRemainingMillis = millisUntilFinished
                TimerController.updateUntilFinished(timerLengthMillis, millisUntilFinished)
                NotificationController.updateTimeLeft(
                    this@TimerService,
                    timerRemainingMillis.toHumanFormat()
                )
            }
        }

    //            val timerInfoIntent = Intent(TIME_INFO).apply {
//                putExtra(INTENT_EXTRA_RESULT, remainingMillis)
//                putExtra(INTENT_EXTRA_INITIAL_TIME, initialTimeInMillis)
//            }
    private fun broadcastIntent(intent: Intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}