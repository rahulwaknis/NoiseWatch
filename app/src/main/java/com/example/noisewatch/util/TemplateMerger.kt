package com.example.noisewatch.util

import com.example.noisewatch.ui.screens.IncidentReportUiState

object TemplateMerger {

    fun mergeTemplate(
        template: String,
        reportState: IncidentReportUiState
    ): String {
        // Split formattedDateTime if possible (e.g. "12 Sep 2026 · 4:50 PM")
        val parts = reportState.formattedDateTime.split(" · ")
        val datePart = parts.getOrNull(0) ?: reportState.formattedDateTime
        val timePart = parts.getOrNull(1) ?: ""

        val secLocParts = reportState.secondaryLocationDetails?.split("  •  ") ?: emptyList()
        val coordsPart = secLocParts.firstOrNull { it.contains(",") } ?: ""
        val accPart = secLocParts.firstOrNull { it.contains("Accuracy") } ?: ""

        val latLongParts = coordsPart.split(", ")
        val latStr = latLongParts.getOrNull(0)?.trim() ?: ""
        val lonStr = latLongParts.getOrNull(1)?.trim() ?: ""

        val hasCoords = latStr.isNotBlank() && lonStr.isNotBlank()
        val hasAcc = accPart.isNotBlank()

        var merged = template
            .replace("{date}", datePart)
            .replace("{time}", timePart)
            .replace("{location}", reportState.primaryLocation)
            .replace("{laeq}", reportState.laeqFormatted)
            .replace("{max}", reportState.maxDbFormatted)
            .replace("{min}", reportState.minDbFormatted)
            .replace("{duration}", reportState.durationFormatted)
            .replace("{source}", reportState.noiseSource ?: "Not specified")
            .replace("{notes}", reportState.notes ?: "None")

        if (hasCoords) {
            merged = merged.replace("{latitude}", latStr).replace("{longitude}", lonStr)
        } else {
            // Cleanly omit Coordinates line if missing
            merged = merged.replace("Coordinates: {latitude}, {longitude}\n", "")
                .replace("Coordinates: {latitude}, {longitude}", "")
                .replace("{latitude}", "")
                .replace("{longitude}", "")
        }

        if (hasAcc) {
            merged = merged.replace("{accuracy}", accPart)
        } else {
            // Cleanly omit Location accuracy line if missing
            merged = merged.replace("Location accuracy: {accuracy}\n", "")
                .replace("Location accuracy: {accuracy}", "")
                .replace("{accuracy}", "")
        }

        return merged
    }

    fun generateXShareText(reportState: IncidentReportUiState): String {
        val parts = reportState.formattedDateTime.split(" · ")
        val datePart = parts.getOrNull(0) ?: reportState.formattedDateTime
        val timePart = parts.getOrNull(1) ?: ""

        return """
Noise incident documented with NoiseWatch
${reportState.primaryLocation}
LAeq: ${reportState.laeqFormatted} dB(A)
Max: ${reportState.maxDbFormatted} dB(A)
$datePart · $timePart

Phone measurement is indicative.
        """.trimIndent()
    }
}
