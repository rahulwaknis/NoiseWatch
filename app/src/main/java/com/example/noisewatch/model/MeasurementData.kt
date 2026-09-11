package com.example.noisewatch.model

data class MeasurementData(
    val laeq: Double,
    val maxDb: Double,
    val minDb: Double = 0.0,
    val durationSeconds: Int,
    val history: List<Double> = emptyList(),
    val hasClipped: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracyMeters: Float? = null,
    val readableAddress: String? = null,
    val locality: String? = null
)
