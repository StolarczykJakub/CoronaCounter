package com.jst.coronacounter.presentation.ui.counter

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.jst.coronacounter.R
import com.jst.coronacounter.presentation.ui.counter.CounterViewState.*
import kotlinx.android.synthetic.main.activity_counter.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class CounterActivity : AppCompatActivity() {

    private val viewModel: CounterViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counter)

        initializeClicks()
        initializeObservers()
    }

    private fun initializeObservers() {
        viewModel.viewState.observe(this, Observer { viewState ->
            when (viewState) {
                is Loading -> {
                    progressBarView.visibility = View.VISIBLE
                }
                is Error -> {
                    progressBarView.visibility = View.GONE
                    Log.d(" ERROR ",  "** ${viewState.message}")
                }
                is DataLoaded -> {
                    progressBarView.visibility = View.GONE
                    deathCounter.text = viewState.data.deaths.toString()
                    confirmedCounter.text = viewState.data.confirmed.toString()
                    recoveredCounter.text = viewState.data.recovered.toString()
                }
            }
        })
    }

    private fun initializeClicks() {
        reloadDataCard.setOnClickListener {
            viewModel.fetchData()
        }
    }
}
