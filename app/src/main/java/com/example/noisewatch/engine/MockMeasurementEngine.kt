package com.example.noisewatch.engine

import com.example.noisewatch.model.MeasurementData
import kotlin.math.log10
import kotlin.math.pow
import kotlin.random.Random

data class MockMeasurementState(
    val currentDb: Double = 72.0,
    val laeq: Double = 72.0,
    val maxDb: Double = 72.0,
    val elapsedSeconds: Int = 0,
    val maxDurationSeconds: Int = 60,
    val history: List<Double> = emptyList(),
    val isFinished: Boolean = false
)

class MockMeasurementEngine {
    private var currentDb = 74.0
    private var elapsedSeconds = 0
    private val maxDuration = 60
    private val history = mutableListOf<Double>()

    fun start(): MockMeasurementState {
        currentDb = Random.nextDouble(68.0, 76.0)
        elapsedSeconds = 0
        history.clear()
        history.add(currentDb)
        return getCurrentState()
    }

    fun tick(): MockMeasurementState {
        if (elapsedSeconds >= maxDuration) {
            return getCurrentState(isFinished = true)
        }

        elapsedSeconds++

        val delta = Random.nextDouble(-3.5, 3.8)
        val spikeChance = Random.nextDouble()
        val spike = if (spikeChance > 0.88) Random.nextDouble(4.0, 8.0) else 0.0

        currentDb = (currentDb + delta + spike).coerceIn(65.0, 95.0)
        history.add(currentDb)

        val isFinished = elapsedSeconds >= maxDuration
        return getCurrentState(isFinished = isFinished)
    }

    fun getCurrentState(isFinished: Boolean = false): MockMeasurementState {
        val maxDb = history.maxOrNull() ?: currentDb
        val laeq = calculateLaeq(history)

        return MockMeasurementState(
            currentDb = currentDb,
            laeq = laeq,
            maxDb = maxDb,
            elapsedSeconds = elapsedSeconds,
            maxDurationSeconds = maxDuration,
            history = history.toList(),
            isFinished = isFinished
        )
    }

    fun getFinalMeasurementData(): MeasurementData {
        val state = getCurrentState()
        return MeasurementData(
            laeq = state.laeq,
            maxDb = state.maxDb,
            durationSeconds = state.elapsedSeconds,
            history = state.history
        )
    }

    private fun calculateLaeq(readings: List<Double>): Double {
        if (readings.isEmpty()) return 0.0
        val linearSum = readings.sumOf { 10.0.pow(it / 10.0) }
        val meanLinear = linearSum / readings.size
        return 10.0 * log10(meanLinear)
    }
}
