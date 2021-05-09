package com.mittylabs.elaps.service

import com.mittylabs.elaps.ui.main.TimerState

typealias onFinishListener = () -> Unit
typealias onTickListener = (Long, Long) -> Unit
typealias onStateChangedListener = (TimerState) -> Unit