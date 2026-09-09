package com.example.noisewatch.model

import com.example.noisewatch.data.local.IncidentEntity

data class Incident(
    val id: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val measurementStartTime: Long = System.currentTimeMillis(),
    val measurementDurationSeconds: Int,
    val laeq: Double,
    val maximumDb: Double,
    val minimumDb: Double,
    val noiseSource: String? = null,
    val notes: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val readableAddress: String? = null,
    val locality: String? = null,
    val photoUri: String? = null
) {
    fun toEntity(): IncidentEntity {
        return IncidentEntity(
            id = id,
            createdAt = createdAt,
            measurementStartTime = measurementStartTime,
            measurementDurationSeconds = measurementDurationSeconds,
            laeq = laeq,
            maximumDb = maximumDb,
            minimumDb = minimumDb,
            noiseSource = noiseSource,
            notes = notes,
            latitude = latitude,
            longitude = longitude,
            readableAddress = readableAddress,
            locality = locality,
            photoUri = photoUri
        )
    }

    companion object {
        fun fromEntity(entity: IncidentEntity): Incident {
            return Incident(
                id = entity.id,
                createdAt = entity.createdAt,
                measurementStartTime = entity.measurementStartTime,
                measurementDurationSeconds = entity.measurementDurationSeconds,
                laeq = entity.laeq,
                maximumDb = entity.maximumDb,
                minimumDb = entity.minimumDb,
                noiseSource = entity.noiseSource,
                notes = entity.notes,
                latitude = entity.latitude,
                longitude = entity.longitude,
                readableAddress = entity.readableAddress,
                locality = entity.locality,
                photoUri = entity.photoUri
            )
        }
    }
}
