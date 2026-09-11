package com.example.noisewatch.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.noisewatch.engine.AudioMeasurementState
import com.example.noisewatch.engine.AudioNoiseEngine
import com.example.noisewatch.location.LocationData
import com.example.noisewatch.location.LocationProvider
import com.example.noisewatch.model.MeasurementData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeasuringViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = AudioNoiseEngine()
    val uiState: StateFlow<AudioMeasurementState> = engine.state

    private val locationProvider = LocationProvider(application)
    private var capturedLocationData: LocationData? = null

    private var captureJob: Job? = null
    private var timerJob: Job? = null
    private var locationJob: Job? = null

    fun startMeasurement() {
        cancelMeasurement() // Clean up any existing session

        capturedLocationData = null

        // Audio capture
        captureJob = viewModelScope.launch {
            engine.startCapture()
        }

        // Location acquisition in background asynchronously
        locationJob = viewModelScope.launch {
            capturedLocationData = locationProvider.getCurrentLocation()
        }

        // 60-second timer
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
        locationJob?.cancel()
        locationJob = null

        locationProvider.stopLocationUpdates()

        val finalData = engine.getFinalMeasurementData()
        engine.stopCapture()

        val loc = capturedLocationData
        return finalData.copy(
            latitude = loc?.latitude,
            longitude = loc?.longitude,
            locationAccuracyMeters = loc?.accuracyMeters,
            readableAddress = loc?.readableAddress,
            locality = loc?.locality
        )
    }

    fun cancelMeasurement() {
        timerJob?.cancel()
        timerJob = null
        captureJob?.cancel()
        captureJob = null
        locationJob?.cancel()
        locationJob = null

        locationProvider.stopLocationUpdates()
        engine.stopCapture()
    }

    override fun onCleared() {
        super.onCleared()
        cancelMeasurement()
    }
}
