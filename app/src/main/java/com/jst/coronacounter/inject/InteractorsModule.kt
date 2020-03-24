package com.jst.coronacounter.inject

import com.jst.coronacounter.presentation.ui.counter.CounterInteractor
import org.koin.dsl.module

val interactorsModule = module {
    single { CounterInteractor(get(), get()) }
}