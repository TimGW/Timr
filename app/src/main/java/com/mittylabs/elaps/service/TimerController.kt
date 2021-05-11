package com.mittylabs.elaps.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startForegroundService
import com.mittylabs.elaps.service.TimerService.Companion.PAUSE_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.START_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.STOP_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.TERMINATE_ACTION
import com.mittylabs.elaps.service.TimerService.Companion.TIMER_LENGTH_EXTRA
import com.mittylabs.elaps.service.TimerService.Companion.TIMER_PAUSED_STATE_EXTRA
import com.mittylabs.elaps.ui.main.TimerState

// TODO refactor this class to use broadcast intent's that the activity can subscribe to
object TimerController : Timer {
    private var finishListener: onFinishListener? = null
    private var tickListener: onTickListener? = null
    private var stateChangedListener: onStateChangedListener? = null

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

    fun updateUntilFinished(timerLengthMillis: Long, millisUntilFinished: Long) {
        tickListener?.invoke(timerLengthMillis, millisUntilFinished)
    }

    fun updateTimerState(timerState: TimerState): TimerState {
        stateChangedListener?.invoke(timerState); return timerState
    }

    class Builder(private val context: Context) {

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