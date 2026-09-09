package com.example.noisewatch.ui.navigation

import com.example.noisewatch.model.MeasurementData

sealed interface Screen {
    data object Measure : Screen
    data object History : Screen
    data object Settings : Screen

    data object Measuring : Screen
    data class IncidentReview(val measurement: MeasurementData) : Screen
    data class Report(val incidentId: Long) : Screen
}
