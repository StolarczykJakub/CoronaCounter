package com.jst.coronacounter.presentation.ui.counter

import android.util.Log
import com.jst.coronacounter.network.NetworkExceptionHandler
import com.jst.coronacounter.network.api.ArcgisApi

class CounterInteractor(
    private val api: ArcgisApi,
    private val networkExceptionHandler: NetworkExceptionHandler
) {
    suspend fun getStatistics(): CounterViewState =
        try {
            CounterViewState.DataLoaded(api.getStatistics().features.first().attributes)
        } catch (exception: Exception) {
            Log.d("Exception ", exception.toString())
            CounterViewState.Error(networkExceptionHandler.handleException(exception))
        }
}