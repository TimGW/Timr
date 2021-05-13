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

object Timer {

    fun Context.play(timeMillis: Long) {
        if (TimerService.timerState is TimerState.Running) return

        Intent(this, TimerService::class.java)
            .apply {
                action = START_ACTION
                putExtra(TIMER_LENGTH_EXTRA, timeMillis)
                putExtra(TIMER_PAUSED_STATE_EXTRA, TimerService.timerState is TimerState.Paused)
            }
            .also { startForegroundService(this, it) }
    }

    fun Context.pause() {
        if (TimerService.timerState !is TimerState.Running) return

        Intent(this, TimerService::class.java)
            .apply { action = PAUSE_ACTION }
            .also { startForegroundService(this, it) }
    }

    fun Context.stop() {
        if (TimerService.timerState is TimerState.Stopped) return

        Intent(this, TimerService::class.java)
            .apply { action = STOP_ACTION }
            .also { startForegroundService(this, it) }
    }

    fun Context.terminate() {
        if (TimerService.timerState is TimerState.Terminated) return

        Intent(this, TimerService::class.java)
            .apply { action = TERMINATE_ACTION }
            .also { startForegroundService(this, it) }
    }
}