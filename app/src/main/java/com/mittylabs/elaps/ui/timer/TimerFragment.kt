package com.mittylabs.elaps.ui.timer

sealed class TimerFragment {
    object Running: TimerFragment()
    object Setup: TimerFragment()
}