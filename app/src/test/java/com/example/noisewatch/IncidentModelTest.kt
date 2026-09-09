package com.example.noisewatch

import com.example.noisewatch.data.local.IncidentEntity
import com.example.noisewatch.model.Incident
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class IncidentModelTest {

    @Test
    fun incident_toEntityAndBack_preservesAllFields() {
        val original = Incident(
            id = 42L,
            createdAt = 1700000000000L,
            measurementStartTime = 1700000000000L,
            measurementDurationSeconds = 60,
            laeq = 81.6,
            maximumDb = 92.4,
            minimumDb = 68.1,
            noiseSource = "Construction",
            notes = "Loud drilling nearby",
            latitude = 18.5204,
            longitude = 73.8567,
            readableAddress = "Erandwane, Pune",
            locality = "Pune",
            photoUri = "content://photo/123"
        )

        val entity = original.toEntity()
        assertEquals(42L, entity.id)
        assertEquals(81.6, entity.laeq, 0.01)
        assertEquals("Construction", entity.noiseSource)

        val converted = Incident.fromEntity(entity)
        assertEquals(original, converted)
    }

    @Test
    fun incidentEntity_creationDefaults() {
        val entity = IncidentEntity(
            measurementDurationSeconds = 60,
            laeq = 76.5,
            maximumDb = 88.0,
            minimumDb = 62.0
        )

        assertEquals(0L, entity.id)
        assertNotNull(entity.createdAt)
        assertEquals(60, entity.measurementDurationSeconds)
    }
}
