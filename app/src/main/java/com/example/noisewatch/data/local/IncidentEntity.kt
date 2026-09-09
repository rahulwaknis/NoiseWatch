package com.example.noisewatch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true)
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
)
