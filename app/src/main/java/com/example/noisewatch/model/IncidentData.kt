package com.example.noisewatch.model

data class IncidentData(
    val measurement: MeasurementData,
    val noiseSource: String? = null,
    val notes: String? = null
)
