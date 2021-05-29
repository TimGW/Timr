package com.mittylabs.timr.timer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mittylabs.timr.model.TimerState

class TimerViewModel : ViewModel() {

    private val _timerState = MutableLiveData<TimerState>()
    val timerState: LiveData<TimerState>
        get() = _timerState

    fun updateTimerState(timerState: TimerState) {
        _timerState.value = timerState
    }
}