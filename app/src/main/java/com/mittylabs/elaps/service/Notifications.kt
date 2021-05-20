package com.mittylabs.elaps.service

import android.app.Notification
import com.mittylabs.elaps.ui.timer.TimerState

interface Notifications {
    fun createNotification(timerLength: Long, timerState: TimerState): Notification
    fun updateTimeLeft(remainingTimeMillis: Long, timerState: TimerState)
    fun updatePauseState(currentTimeRemaining: Long, timerState: TimerState)
    fun updateStopState(initialTimerLength: Long, timerState: TimerState)
    fun updateFinishedState(elapsedTime: Long, timerState: TimerState)
    fun removeNotifications()
}
