package com.jst.coronacounter.inject

import com.jst.coronacounter.presentation.ui.counter.CounterViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { CounterViewModel(get()) }
}