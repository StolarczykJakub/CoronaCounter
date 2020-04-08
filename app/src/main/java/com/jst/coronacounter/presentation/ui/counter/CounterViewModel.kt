package com.jst.coronacounter.presentation.ui.counter

import androidx.lifecycle.*
import com.jst.coronacounter.data.Statistics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CounterViewModel(private val counterInteractor: CounterInteractor) : ViewModel() {

    val viewState = MutableLiveData<CounterViewState>()

    fun fetchData() {
        viewState.value = CounterViewState.Loading
        viewModelScope.launch {
            delay(1200)
            viewState.postValue(counterInteractor.getStatistics())
        }
    }
}

sealed class CounterViewState {
    class DataLoaded(val data: Statistics) : CounterViewState()
    class Error(val message: String) : CounterViewState()
    object Loading : CounterViewState()
}

