package com.mittylabs.elaps.prefs

import com.mittylabs.elaps.ui.main.TimerState

class SharedPrefs(
    private val preferences: SharedPrefManager
) {

    fun getTimerLength(): Int {
        return preferences.getIntValue(TIMER_LENGTH_ID, 10)
    }

    fun getPreviousTimerLengthSeconds(): Long {
        return preferences.getLongValue(PREVIOUS_TIMER_LENGTH_SECONDS_ID, 0)
    }
    fun setPreviousTimerLengthSeconds(seconds: Long) {
        preferences.setLongValue(PREVIOUS_TIMER_LENGTH_SECONDS_ID, seconds)
    }

    fun getTimerState(): TimerState {
        val ordinal = preferences.getIntValue(TIMER_STATE_ID, 0)
        return TimerState.values()[ordinal]
    }
    fun setTimerState(state: TimerState) {
        preferences.setIntValue(TIMER_STATE_ID, state.ordinal)
    }

    fun getSecondsRemaining(): Long {
        return preferences.getLongValue(SECONDS_REMAINING_ID, 0)
    }
    fun setSecondsRemaining(seconds: Long) {
        preferences.setLongValue(SECONDS_REMAINING_ID, seconds)
    }

    fun getAlarmSetTime(): Long {
        return preferences.getLongValue(ALARM_SET_TIME_ID, 0)
    }
    fun setAlarmSetTime(time: Long) {
        preferences.setLongValue(ALARM_SET_TIME_ID, time)
    }

    companion object {
        private const val TIMER_LENGTH_ID = "timer_length"
        private const val PREVIOUS_TIMER_LENGTH_SECONDS_ID = "previous_timer_length_seconds"
        private const val TIMER_STATE_ID = "timer_state"
        private const val SECONDS_REMAINING_ID = "seconds_remaining"
        private const val ALARM_SET_TIME_ID = "backgrounded_time"
    }
}
