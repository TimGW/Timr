package com.mittylabs.elaps.timer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mittylabs.elaps.model.TimerState

class TimerViewModel : ViewModel() {

    private val _timerState = MutableLiveData<TimerState?>()
    val timerState: LiveData<TimerState?>
        get() = _timerState

    fun updateTimerState(timerState: TimerState?) {
        _timerState.value = timerState
    }
}