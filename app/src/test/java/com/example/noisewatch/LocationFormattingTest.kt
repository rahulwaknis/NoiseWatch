package com.example.noisewatch

import com.example.noisewatch.data.ComplaintPreferences
import com.example.noisewatch.model.Incident
import com.example.noisewatch.ui.screens.toReportUiState
import com.example.noisewatch.util.TemplateMerger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class LocationFormattingTest {

    @Test
    fun incident_toEntityAndBack_preservesAccuracyAndLocationFields() {
        val original = Incident(
            id = 100L,
            measurementDurationSeconds = 60,
            laeq = 78.5,
            maximumDb = 89.2,
            minimumDb = 65.0,
            latitude = 18.5167,
            longitude = 73.8563,
            locationAccuracyMeters = 8.4f,
            readableAddress = "Erandwane, Pune, Maharashtra 411004",
            locality = "Erandwane, Pune"
        )

        val entity = original.toEntity()
        assertEquals(18.5167, entity.latitude!!, 0.0001)
        assertEquals(73.8563, entity.longitude!!, 0.0001)
        assertEquals(8.4f, entity.locationAccuracyMeters!!, 0.01f)
        assertEquals("Erandwane, Pune", entity.locality)

        val restored = Incident.fromEntity(entity)
        assertEquals(original, restored)
    }

    @Test
    fun locationDisplayFallback_localityPriority() {
        val incidentWithLocality = Incident(
            measurementDurationSeconds = 60,
            laeq = 78.5,
            maximumDb = 89.2,
            minimumDb = 65.0,
            latitude = 18.5167,
            longitude = 73.8563,
            readableAddress = "123 Main St, Pune",
            locality = "Erandwane, Pune"
        )

        val historyText = when {
            !incidentWithLocality.locality.isNullOrBlank() -> incidentWithLocality.locality
            !incidentWithLocality.readableAddress.isNullOrBlank() -> incidentWithLocality.readableAddress
            incidentWithLocality.latitude != null && incidentWithLocality.longitude != null -> "Location captured"
            else -> "Location not captured"
        }

        assertEquals("Erandwane, Pune", historyText)
    }

    @Test
    fun locationDisplayFallback_addressFallback() {
        val incidentWithAddressOnly = Incident(
            measurementDurationSeconds = 60,
            laeq = 78.5,
            maximumDb = 89.2,
            minimumDb = 65.0,
            latitude = 18.5167,
            longitude = 73.8563,
            readableAddress = "Full Formatted Address",
            locality = null
        )

        val historyText = when {
            !incidentWithAddressOnly.locality.isNullOrBlank() -> incidentWithAddressOnly.locality
            !incidentWithAddressOnly.readableAddress.isNullOrBlank() -> incidentWithAddressOnly.readableAddress
            incidentWithAddressOnly.latitude != null && incidentWithAddressOnly.longitude != null -> "Location captured"
            else -> "Location not captured"
        }

        assertEquals("Full Formatted Address", historyText)
    }

    @Test
    fun locationDisplayFallback_coordinatesOnlyFallback() {
        val incidentWithCoordsOnly = Incident(
            measurementDurationSeconds = 60,
            laeq = 78.5,
            maximumDb = 89.2,
            minimumDb = 65.0,
            latitude = 18.5167,
            longitude = 73.8563,
            readableAddress = null,
            locality = null
        )

        val historyText = when {
            !incidentWithCoordsOnly.locality.isNullOrBlank() -> incidentWithCoordsOnly.locality
            !incidentWithCoordsOnly.readableAddress.isNullOrBlank() -> incidentWithCoordsOnly.readableAddress
            incidentWithCoordsOnly.latitude != null && incidentWithCoordsOnly.longitude != null -> "Location captured"
            else -> "Location not captured"
        }

        assertEquals("Location captured", historyText)
    }

    @Test
    fun locationDisplayFallback_noLocation() {
        val incidentNoLocation = Incident(
            measurementDurationSeconds = 60,
            laeq = 78.5,
            maximumDb = 89.2,
            minimumDb = 65.0
        )

        val historyText = when {
            !incidentNoLocation.locality.isNullOrBlank() -> incidentNoLocation.locality
            !incidentNoLocation.readableAddress.isNullOrBlank() -> incidentNoLocation.readableAddress
            incidentNoLocation.latitude != null && incidentNoLocation.longitude != null -> "Location captured"
            else -> "Location not captured"
        }

        assertEquals("Location not captured", historyText)
    }

    @Test
    fun accuracyFormatting_roundsToNearestMetre() {
        val accuracy = 8.4f
        val formatted = "Accuracy ±${accuracy.roundToInt()} m"
        assertEquals("Accuracy ±8 m", formatted)
    }

    @Test
    fun reportUiState_mappingAndOptionalSectionsHandling() {
        val incident = Incident(
            id = 5L,
            createdAt = 1700000000000L,
            measurementDurationSeconds = 60,
            laeq = 60.4,
            maximumDb = 63.5,
            minimumDb = 42.4,
            noiseSource = "",
            notes = null,
            latitude = 18.5114,
            longitude = 73.8168,
            locationAccuracyMeters = 10.0f,
            locality = "Kothrud, Pune"
        )

        val uiState = incident.toReportUiState()
        assertEquals("60.4", uiState.laeqFormatted)
        assertEquals("63.5", uiState.maxDbFormatted)
        assertEquals("42.4", uiState.minDbFormatted)
        assertEquals("Kothrud, Pune", uiState.primaryLocation)
        assertEquals(false, uiState.isHighNoise)
        assertNull(uiState.noiseSource)
        assertNull(uiState.notes)
        assertNull(uiState.photoUri)
    }

    @Test
    fun templateMerger_withAndWithoutCoordinates_handlesPlaceholdersCleanly() {
        val incidentWithCoords = Incident(
            id = 5L,
            createdAt = 1700000000000L,
            measurementDurationSeconds = 60,
            laeq = 78.8,
            maximumDb = 84.2,
            minimumDb = 55.0,
            noiseSource = "DJ / amplified music",
            notes = "Loud music near residential area",
            latitude = 18.5114,
            longitude = 73.8168,
            locationAccuracyMeters = 100.0f,
            locality = "Kothrud, Pune"
        )

        val stateWithCoords = incidentWithCoords.toReportUiState()
        val template = ComplaintPreferences.DEFAULT_BODY_TEMPLATE
        val mergedWithCoords = TemplateMerger.mergeTemplate(template, stateWithCoords)

        assertTrue(mergedWithCoords.contains("Location: Kothrud, Pune"))
        assertTrue(mergedWithCoords.contains("Coordinates: 18.5114, 73.8168"))
        assertTrue(mergedWithCoords.contains("Location accuracy: Accuracy ±100 m"))
        assertTrue(mergedWithCoords.contains("Noise Pollution (Regulation and Control) Rules, 2000"))

        val incidentNoCoords = Incident(
            id = 6L,
            createdAt = 1700000000000L,
            measurementDurationSeconds = 60,
            laeq = 60.0,
            maximumDb = 65.0,
            minimumDb = 40.0,
            locality = "Location not captured"
        )
        val stateNoCoords = incidentNoCoords.toReportUiState()
        val mergedNoCoords = TemplateMerger.mergeTemplate(template, stateNoCoords)

        assertFalse(mergedNoCoords.contains("{latitude}"))
        assertFalse(mergedNoCoords.contains("{longitude}"))
        assertFalse(mergedNoCoords.contains("{accuracy}"))
        assertFalse(mergedNoCoords.contains("Coordinates:"))
        assertFalse(mergedNoCoords.contains("Location accuracy:"))
    }
}
