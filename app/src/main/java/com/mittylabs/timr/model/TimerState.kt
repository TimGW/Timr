package com.mittylabs.timr.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class TimerState : Parcelable {
    abstract val isPlayIconVisible: Boolean

    @Parcelize
    data class Started(
        val timerLength: Long,
        val timerRemaining: Long,
    ) : TimerState() {
        override val isPlayIconVisible: Boolean
            get() = false
    }

    @Parcelize
    data class Paused(
        val timerLength: Long,
        val timerRemaining: Long,
    ) : TimerState() {
        override val isPlayIconVisible: Boolean
            get() = true
    }

    @Parcelize
    data class Stopped(
        val timerLength: Long,
    ) : TimerState() {
        override val isPlayIconVisible: Boolean
            get() = true
    }

    @Parcelize
    data class Finished(
        val elapsedTime: Long,
    ) : TimerState() {
        override val isPlayIconVisible: Boolean
            get() = true
    }

    @Parcelize
    object Terminated : TimerState() {
        override val isPlayIconVisible: Boolean
            get() = true
    }
}