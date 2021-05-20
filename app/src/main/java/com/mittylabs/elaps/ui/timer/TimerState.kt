package com.mittylabs.elaps.ui.timer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class TimerState : Parcelable {

    @Parcelize
    data class Started(
        val currentTimerLength: Long,
        val currentTimeRemaining: Long,
        val isPlayIconVisible: Boolean
    ) : TimerState()

    @Parcelize
    data class Progress(
        val currentTimerLength: Long,
        val currentTimeRemaining: Long,
    ) : TimerState()

    @Parcelize
    data class Paused(
        val currentTimerLength: Long,
        val currentTimeRemaining: Long,
        val isPlayIconVisible: Boolean
    ) : TimerState()

    @Parcelize
    data class Stopped(
        val initialTimerLength: Long,
        val isPlayIconVisible: Boolean
    ) : TimerState()

    @Parcelize
    data class Finished(
        val isPlayIconVisible: Boolean,
        val elapsedTime: Long
    ) : TimerState()

    @Parcelize
    object Terminated : TimerState()
}