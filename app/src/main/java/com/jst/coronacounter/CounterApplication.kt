package com.jst.coronacounter

import android.app.Application
import com.jst.coronacounter.inject.interactorsModule
import com.jst.coronacounter.inject.networkModule
import com.jst.coronacounter.inject.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class CounterApplication : Application() {
    override fun onCreate(){
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@CounterApplication)
            modules(networkModule, interactorsModule, viewModelModule)
        }
    }
}