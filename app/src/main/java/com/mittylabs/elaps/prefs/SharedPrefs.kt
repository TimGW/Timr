package com.mittylabs.elaps.prefs

import com.mittylabs.elaps.ui.main.TimerState

class SharedPrefs(
    private val preferences: SharedPrefManager
) {

    fun getTimerLength(): Long {
        return preferences.getLongValue(TIMER_LENGTH_ID, 0L)
    }
    fun setTimerLength(length: Long) {
        preferences.setLongValue(TIMER_STATE_ID, length)
    }

    fun getTimerState(): TimerState {
        val ordinal = preferences.getIntValue(TIMER_STATE_ID, 0)
        return TimerState.values()[ordinal]
    }
    fun setTimerState(state: TimerState) {
        preferences.setIntValue(TIMER_STATE_ID, state.ordinal)
    }

    fun getTimeRemaining(): Long {
        return preferences.getLongValue(MILLISECONDS_REMAINING_ID, 0)
    }
    fun setTimeRemaining(milliseconds: Long) {
        preferences.setLongValue(MILLISECONDS_REMAINING_ID, milliseconds)
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
        private const val MILLISECONDS_REMAINING_ID = "milliseconds_remaining"
        private const val ALARM_SET_TIME_ID = "backgrounded_time"
    }
}
