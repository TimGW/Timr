package com.mittylabs.elaps.ui.main

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class TimerState: Parcelable {
    abstract val initialTime: Long
    abstract var remainingTime: Long

    @Parcelize data class Initialize(
        override val initialTime: Long,
        override var remainingTime: Long,
    ) : TimerState()

    @Parcelize data class Running(
        override val initialTime: Long,
        override var remainingTime: Long
    ) : TimerState()

    @Parcelize data class Paused(
        override val initialTime: Long,
        override var remainingTime: Long
    ) : TimerState()

    @Parcelize data class Stopped(
        override val initialTime: Long,
        override var remainingTime: Long
    ) : TimerState()

    @Parcelize data class Finished(
        override val initialTime: Long,
        override var remainingTime: Long,
        val elapsedTime: Long
    ) : TimerState()

    @Parcelize data class Terminated(
        override val initialTime: Long,
        override var remainingTime: Long
    ) : TimerState()
}