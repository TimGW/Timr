package com.mittylabs.timr.notification

import android.app.Notification
import com.mittylabs.timr.model.TimerState

interface Notifications {
    fun getOrCreateNotification(timerLength: Long, timerState: TimerState): Notification
    fun updateTimeLeft(remainingTimeMillis: Long, timerState: TimerState)
    fun updatePauseState(currentTimeRemaining: Long, timerState: TimerState)
    fun updateStopState(initialTimerLength: Long, timerState: TimerState)
    fun updateFinishedState(elapsedTime: Long, timerState: TimerState)
    fun removeNotifications()
}
