package com.example.noisewatch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noisewatch.engine.AudioMeasurementState
import com.example.noisewatch.engine.AudioNoiseEngine
import com.example.noisewatch.model.MeasurementData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeasuringViewModel : ViewModel() {

    private val engine = AudioNoiseEngine()
    val uiState: StateFlow<AudioMeasurementState> = engine.state

    private var captureJob: Job? = null
    private var timerJob: Job? = null

    fun startMeasurement() {
        cancelMeasurement() // Clean up any existing session

        captureJob = viewModelScope.launch {
            engine.startCapture()
        }

        timerJob = viewModelScope.launch {
            var elapsed = 0
            while (elapsed < 60) {
                delay(1000L)
                elapsed++
                engine.updateTimer(elapsedSeconds = elapsed, maxDurationSeconds = 60)
            }
        }
    }

    fun stopMeasurement(): MeasurementData {
        timerJob?.cancel()
        timerJob = null
        captureJob?.cancel()
        captureJob = null
        val finalData = engine.getFinalMeasurementData()
        engine.stopCapture()
        return finalData
    }

    fun cancelMeasurement() {
        timerJob?.cancel()
        timerJob = null
        captureJob?.cancel()
        captureJob = null
        engine.stopCapture()
    }

    override fun onCleared() {
        super.onCleared()
        cancelMeasurement()
    }
}
