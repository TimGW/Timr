package com.mittylabs.elaps.ui.timer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mittylabs.elaps.prefs.SharedPrefs
import com.mittylabs.elaps.utils.Event

class TimerViewModel(
    sharedPrefs: SharedPrefs
) : ViewModel() {

    private val default = if(sharedPrefs.isTimerServiceRunning()) {
        TimerFragment.Running
    } else {
        TimerFragment.Setup
    }

    private val _openFragment = MutableLiveData(Event(default))
    val openFragment: LiveData<Event<TimerFragment>>
        get() = _openFragment

    private val _timerState = MutableLiveData<TimerState>()
    val timerState: LiveData<TimerState>
        get() = _timerState

    private val _timerStart = MutableLiveData<Event<Long>>()
    val timerStart: LiveData<Event<Long>>
        get() = _timerStart

    private val _toolbarTitle = MutableLiveData<String>()
    val toolbarTitle: LiveData<String>
        get() = _toolbarTitle

    fun updateOpenFragment(fragment: TimerFragment) {
        _openFragment.value = Event(fragment)
    }

    fun updateTimerState(timerState: TimerState) {
        _timerState.value = timerState
    }

    fun updateTimerStart(time: Long) {
         _timerStart.value = Event(time)
    }

    fun updateToolbarTitle(title: String) {
        _toolbarTitle.value = title
    }
}