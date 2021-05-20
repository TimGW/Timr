package com.mittylabs.elaps

import com.mittylabs.elaps.ui.timer.TimerViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { TimerViewModel(get()) }
}
